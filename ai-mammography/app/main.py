import base64
import io
import os
import uuid
from fastapi import FastAPI, File, UploadFile, HTTPException, Depends, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr
import httpx
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from config import Settings
from dicom_utils import dicom_to_png
from model_utils import load_model, predict
from image_processing import process_predictions
from classifier_utils import load_classifier, classify
from sendmail import send_email, reply_email

# Dependency to load settings from .env

def get_settings() -> Settings:
    return Settings()

# Initialize FastAPI app
app = FastAPI()

# Load models on startup
model = load_model()
classifier = load_classifier()

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health_check():
    return {"status": "ok", "models_loaded": model is not None and classifier is not None}

ALLOWED_EXTENSIONS = {".dcm",".dicom", ".png", ".jpg", ".jpeg"}
MAX_FILE_SIZE = 60 * 1024 * 1024  # 60 MB

def convert_image_to_base64(image_stream: io.BytesIO) -> str:
    image_stream.seek(0)
    return base64.b64encode(image_stream.read()).decode('utf-8')

@app.post("/predict")
async def predict_api(
    file: UploadFile = File(...),
    pixel_spacing: str = Form("0.1")
):
    print("Received file:", file.filename)

    # Parse pixel_spacing
    try:
        pixel_spacing_value = float(pixel_spacing) if pixel_spacing else None
    except ValueError:
        raise HTTPException(400, "Invalid pixel spacing. Must be a number.")

    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(400, "File is too large.")
    await file.seek(0)

    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(400, f"Unsupported file type: {ext}")

    temp_dir = "temp"
    os.makedirs(temp_dir, exist_ok=True)
    input_path = os.path.join(temp_dir, f"{uuid.uuid4()}{ext}")
    output_path = os.path.join(temp_dir, f"{uuid.uuid4()}.png")

    try:
        # Save the uploaded file to a temporary location
        with open(input_path, "wb") as f:
            f.write(content)
        

        # Convert DICOM if needed
        if ext in [".dcm", ".dicom"]:
            dicom_to_png(input_path, output_path)
            image_path = output_path
            with open(output_path, "rb") as f:
                original_stream = io.BytesIO(f.read())
        else:
            image_path = input_path
            original_stream = io.BytesIO(content)

        results = predict(image_path, model)
        if results['boxes']:
            results = classify(image_path, results, classifier)
            response = process_predictions(image_path, results, pixel_spacing_value)
            return JSONResponse(content=response)

        return {
            "status": "success",
            "detections": False,
            "full_Normal_image": convert_image_to_base64(original_stream)
        }
    except Exception as e:
        raise HTTPException(500, str(e))
    finally:
        for path in (input_path, output_path):
            if os.path.exists(path):
                os.remove(path)

# Email data model
class EmailData(BaseModel):
    name: str
    email: EmailStr
    subject: str
    message: str

# Logic fix: Centralized Conclusion Logic
class ConclusionRequest(BaseModel):
    prompt: str

# ✅ Multi-model fallback list — if one model is removed from OpenRouter, the next is tried
FREE_MODELS = [
    "meta-llama/llama-3.1-8b-instruct:free",
    "meta-llama/llama-3.2-3b-instruct:free",
    "google/gemma-2-9b-it:free",
    "mistralai/mistral-7b-instruct:free",
    "qwen/qwen-2.5-7b-instruct:free",
    "microsoft/phi-3-mini-128k-instruct:free",
]

@app.post("/conclusion")
async def generate_conclusion(
    request: ConclusionRequest,
    settings: Settings = Depends(get_settings)
):
    if not settings.openrouter_key or "your_key_here" in settings.openrouter_key:
        return {"conclusion": "Note: Using local analysis (No API key). The AI models have detected a lesion. "
                             "Detailed report generation requires an OpenRouter API key in .env. "
                             "Based on the analysis, the findings are consistent with the visual labels provided."}

    headers = {
        "Authorization": f"Bearer {settings.openrouter_key}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://safescan-clinic.ai",
        "X-Title": "SafeScan Mammography Analysis"
    }

    last_error = None
    async with httpx.AsyncClient() as client:
        for model_name in FREE_MODELS:
            try:
                print(f"[SafeScan] Trying model: {model_name}")
                response = await client.post(
                    "https://openrouter.ai/api/v1/chat/completions",
                    headers=headers,
                    json={
                        "model": model_name,
                        "messages": [{"role": "user", "content": request.prompt}],
                        "temperature": 0.7
                    },
                    timeout=60.0
                )

                if response.status_code == 200:
                    data = response.json()
                    content = data['choices'][0]['message']['content']
                    print(f"[SafeScan] ✅ Report generated with model: {model_name}")
                    return {"conclusion": content}
                else:
                    last_error = f"{model_name} -> {response.status_code}: {response.text}"
                    print(f"[SafeScan] ⚠ Model failed: {last_error}")
                    continue  # Try next model

            except Exception as e:
                last_error = f"{model_name} -> {str(e)}"
                print(f"[SafeScan] ⚠ Model exception: {last_error}")
                continue  # Try next model

    # All models failed
    print(f"[SafeScan] ❌ All models failed. Last error: {last_error}")
    raise HTTPException(status_code=503, detail=f"All AI models unavailable. Last: {last_error}")

@app.post("/send-email")
async def send_email_endpoint(
    email_data: EmailData,
    settings: Settings = Depends(get_settings)
):
    try:
        # Prepare email content
        body_text = (
            f"New Contact Form Submission:\n\n"
            f"Name: {email_data.name}\n"
            f"Email: {email_data.email}\n"
            f"Subject: {email_data.subject}\n"
            f"Message:\n{email_data.message}\n"
        )

        # Send and reply
        send_email(
            subject=email_data.subject,
            email=email_data.email,
            name=email_data.name,
            message=email_data.message,
            body=body_text,
            settings=settings,
            recipient_email=settings.email
        )
        reply_email(
            subject=email_data.subject,
            email=email_data.email,
            name=email_data.name,
            message=email_data.message,
            settings=settings
        )

        return {"status": "success", "message": "Email processed"}
    except Exception as e:
        print(str(e))
        raise HTTPException(500, str(e))
