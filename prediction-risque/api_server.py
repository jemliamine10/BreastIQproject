"""
FastAPI Microservice — METABRIC Breast Cancer Recurrence Risk Prediction
========================================================================
Loads the trained CatBoost model (perfect_model.pkl) and exposes a REST API
for the Spring Boot backend to call.

Port: 8002 (separate from mammogram AI on 8000)
"""

import json
import pickle
import logging
from pathlib import Path
from typing import Optional

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# ── Config ──
BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "trained_model" / "perfect_model.pkl"
META_PATH = BASE_DIR / "trained_model" / "model_metadata.json"
STATS_PATH = BASE_DIR / "dataset_stats.json"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("RiskPredictionAPI")

# ── Load model at startup ──
model = None
metadata = None
dataset_stats = None


def load_resources():
    global model, metadata, dataset_stats
    logger.info("Loading model from %s", MODEL_PATH)
    with open(MODEL_PATH, "rb") as f:
        model = pickle.load(f)
    with open(META_PATH, "r") as f:
        metadata = json.load(f)
    with open(STATS_PATH, "r") as f:
        dataset_stats = json.load(f)
    logger.info("Model v%s loaded. Features: %d, Threshold: %.4f",
                metadata["version"], len(metadata["features"]), metadata["optimized_threshold"])


# ── Pydantic Models ──
class RiskPredictionRequest(BaseModel):
    age_at_diagnosis: float = Field(..., ge=18, le=110, description="Age at diagnosis")
    type_of_breast_surgery: str = Field(default="Unknown", description="Surgery type")
    cellularity: str = Field(default="Unknown", description="Tumor cellularity")
    chemotherapy: str = Field(default="Unknown", description="Chemotherapy received")
    pam50_claudin_low_subtype: str = Field(default="Unknown", description="PAM50 molecular subtype")
    er_status_measured_by_ihc: str = Field(default="Unknown", description="ER IHC status")
    er_status: str = Field(default="Unknown", description="Estrogen receptor status")
    neoplasm_histologic_grade: float = Field(default=2.0, ge=1, le=3, description="Histologic grade (1-3)")
    her2_status_measured_by_snp6: str = Field(default="Unknown", description="HER2 SNP6 status")
    her2_status: str = Field(default="Unknown", description="HER2 status")
    tumor_other_histologic_subtype: str = Field(default="Unknown", description="Histologic subtype")
    hormone_therapy: str = Field(default="Unknown", description="Hormone therapy received")
    inferred_menopausal_state: str = Field(default="Unknown", description="Menopausal state")
    integrative_cluster: str = Field(default="Unknown", description="Integrative cluster")
    primary_tumor_laterality: str = Field(default="Unknown", description="Tumor laterality")
    lymph_nodes_examined_positive: float = Field(default=0.0, ge=0, description="Positive lymph nodes count")
    mutation_count: float = Field(default=5.0, ge=0, description="Mutation count")
    nottingham_prognostic_index: float = Field(default=4.0, ge=0, description="Nottingham prognostic index")
    pr_status: str = Field(default="Unknown", description="Progesterone receptor status")
    radio_therapy: str = Field(default="Unknown", description="Radiotherapy received")
    three_gene_classifier_subtype: str = Field(default="Unknown", alias="3_gene_classifier_subtype",
                                                description="3-gene classifier subtype")
    tumor_size: float = Field(default=25.0, ge=0, description="Tumor size in mm")
    tumor_stage: float = Field(default=2.0, ge=0, le=4, description="Tumor stage (0-4)")

    class Config:
        populate_by_name = True


class RiskPredictionResponse(BaseModel):
    probability: float
    probability_percent: float
    is_high_risk: bool
    risk_level: str  # "LOW", "MODERATE", "HIGH", "VERY_HIGH"
    threshold: float
    model_version: str
    features_used: int
    model_metrics: dict


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_version: Optional[str]
    features_count: Optional[int]
    threshold: Optional[float]


class DatasetStatsResponse(BaseModel):
    stats: dict


# ── FastAPI App ──
app = FastAPI(
    title="METABRIC Recurrence Risk Prediction API",
    description="Predicts breast cancer recurrence risk using a calibrated CatBoost model",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup():
    load_resources()


@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="ok" if model is not None else "error",
        model_loaded=model is not None,
        model_version=metadata["version"] if metadata else None,
        features_count=len(metadata["features"]) if metadata else None,
        threshold=metadata["optimized_threshold"] if metadata else None
    )


@app.get("/dataset-stats", response_model=DatasetStatsResponse)
async def get_dataset_stats():
    """Return the dataset statistics (categorical values for dropdowns)."""
    if dataset_stats is None:
        raise HTTPException(status_code=503, detail="Dataset stats not loaded")
    return DatasetStatsResponse(stats=dataset_stats)


@app.post("/predict-risk", response_model=RiskPredictionResponse)
async def predict_risk(request: RiskPredictionRequest):
    """Predict recurrence risk from 22 METABRIC features."""
    if model is None or metadata is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    features_ordered = metadata["features"]
    threshold = metadata["optimized_threshold"]

    # Build input dict, handling the "3_gene_classifier_subtype" alias
    input_dict = request.model_dump(by_alias=True)

    # Ensure the 3_gene key is present
    if "3_gene_classifier_subtype" not in input_dict and "three_gene_classifier_subtype" in input_dict:
        input_dict["3_gene_classifier_subtype"] = input_dict.pop("three_gene_classifier_subtype")

    # Build DataFrame in correct column order
    try:
        input_df = pd.DataFrame([input_dict])[features_ordered]
    except KeyError as e:
        raise HTTPException(status_code=400, detail=f"Missing feature: {e}")

    # Predict
    try:
        probas = model.predict_proba(input_df)[0, 1]
    except Exception as e:
        logger.error("Prediction error: %s", e)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

    probability = float(probas)
    is_high_risk = probability >= threshold

    # Determine risk level
    if probability < 0.2:
        risk_level = "LOW"
    elif probability < threshold:
        risk_level = "MODERATE"
    elif probability < 0.6:
        risk_level = "HIGH"
    else:
        risk_level = "VERY_HIGH"

    return RiskPredictionResponse(
        probability=round(probability, 4),
        probability_percent=round(probability * 100, 1),
        is_high_risk=is_high_risk,
        risk_level=risk_level,
        threshold=round(threshold, 4),
        model_version=metadata["version"],
        features_used=len(features_ordered),
        model_metrics=metadata["metrics_test"]
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)
