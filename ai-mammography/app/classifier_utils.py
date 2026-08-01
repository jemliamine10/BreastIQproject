import numpy as np
import joblib
import cv2
import os
from sklearn.preprocessing import StandardScaler
from keras.applications.densenet import DenseNet121, preprocess_input as dpi
from keras.applications.convnext import ConvNeXtTiny, preprocess_input as cpi
import matplotlib.pyplot as plt
import torch

# Force CPU if no CUDA available
if not torch.cuda.is_available():
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"

# Load models and scaler at startup
print("Loading models and scaler...")
model_densenet = DenseNet121(weights='imagenet', include_top=False, input_shape=(224, 224, 3))
model_convnext = ConvNeXtTiny(weights='imagenet', include_top=False, input_shape=(224, 224, 3))
scaler = joblib.load(os.path.join("models", "scaler.joblib"))

# Predefined crop sizes for dynamic ROI cropping
CROP_SIZES = [112, 224, 512, 750, 1024, 1500]

def load_classifier():
    """Load the trained classification model."""
    print('Loading classifier model...')
    return joblib.load(os.path.join("models", "svm+lgbmdensenet+convnexttiny+9720+c=6+k=sigmoid+augmentation.pkl"))

def smart_crop_from_box(orig_cv, box, target_size=224):
    """
    Crop a region around the bounding box with increasing sizes until the box fits.
    Falls back to resizing the whole image if needed.
    """
    height, width = orig_cv.shape[:2]
    x1, y1, x2, y2 = map(int, box)
    box_width, box_height = x2 - x1, y2 - y1

    max_dim = max(box_width, box_height)

    for crop_size in CROP_SIZES:
        if crop_size < max_dim:
            continue  # Skip if crop is too small

        # Compute center of box and propose crop
        box_center_x, box_center_y = (x1 + x2) // 2, (y1 + y2) // 2
        crop_x1 = max(0, box_center_x - crop_size // 2)
        crop_y1 = max(0, box_center_y - crop_size // 2)
        crop_x2 = min(width, crop_x1 + crop_size)
        crop_y2 = min(height, crop_y1 + crop_size)

        # Adjust crop to fit inside image
        if crop_x2 - crop_x1 < crop_size:
            crop_x1 = max(0, crop_x2 - crop_size)
        if crop_y2 - crop_y1 < crop_size:
            crop_y1 = max(0, crop_y2 - crop_size)

        # If full box is inside crop, return resized crop
        if x1 >= crop_x1 and y1 >= crop_y1 and x2 <= crop_x2 and y2 <= crop_y2:
            cropped = orig_cv[crop_y1:crop_y2, crop_x1:crop_x2]
            return cv2.resize(cropped, (target_size, target_size), interpolation=cv2.INTER_LINEAR)

    # If all else fails, resize full image
    return cv2.resize(orig_cv, (target_size, target_size), interpolation=cv2.INTER_LINEAR)

def extract_deep_features(image_batch):
    """
    Extract features using DenseNet121 and ConvNeXtTiny,
    then concatenate their flattened outputs.
    """
    features_dense = model_densenet.predict(dpi(image_batch.copy()), verbose=0)
    features_conv = model_convnext.predict(cpi(image_batch.copy()), verbose=0)

    return np.hstack([
        features_dense.reshape(features_dense.shape[0], -1),
        features_conv.reshape(features_conv.shape[0], -1)
    ])

def classify(image_path, results, classifier):
    """
    Classify lesions (Benign/Malignant) based on cropped regions from detection boxes.
    """
    try:
        orig_cv = cv2.imread(image_path)
        if orig_cv is None:
            raise ValueError(f"Could not read image at {image_path}")

        # Resize input image to expected dimension
        resized_cv = cv2.resize(orig_cv, (957, 1147))
        scale_x = 957 / orig_cv.shape[1]
        scale_y = 1147 / orig_cv.shape[0]
        resized_rgb = cv2.cvtColor(resized_cv, cv2.COLOR_BGR2RGB)

        cropped_images = []
        for box, score in zip(results['boxes'], results['scores']):
            if score < 0.5:
                continue  # Ignore low-confidence detections

            # Scale box to resized image
            scaled_box = (np.array(box) * [scale_x, scale_y, scale_x, scale_y]).astype(int)

            # Crop region and resize
            cropped_img = smart_crop_from_box(resized_rgb, scaled_box)
            cropped_images.append(cropped_img)

        if cropped_images:
            # Create batch of cropped inputs
            batch_images = np.array(cropped_images)

            # Extract features and classify
            features = extract_deep_features(batch_images)
            predictions = classifier.predict(scaler.transform(features))
            results['classification'] = ['Benign' if p == 0 else 'Malignant' for p in predictions]

        return results

    except Exception as e:
        print(f"Error in classification: {str(e)}")
        results['classification'] = []
        return results
