import base64
from fastapi.responses import JSONResponse
import numpy as np
import cv2
from PIL import Image
import torch
from skimage.color import rgb2gray
import matplotlib.pyplot as plt
from skimage import morphology, measure, feature
from skimage.measure import regionprops, perimeter_crofton

# Custom classes and visualization colors
CUSTOM_CLASSES = ['calc', 'mass']
COLOR = [(209, 109, 145), (0, 255, 255)]  # BGR colors for bounding boxes and masks
CONTOUR_COLOR = [(0, 255, 255), (209, 109, 145)]  # BGR colors for mask contours

def draw_rounded_rectangle(img, top_left, bottom_right, color, corner_radius=10, thickness=-1, alpha=0.6):
    """
    Draws a rounded rectangle with adjustable transparency on an image.
    """
    overlay = img.copy()
    x1, y1 = top_left
    x2, y2 = bottom_right

    # Draw the straight sides of the rectangle
    cv2.rectangle(overlay, (x1 + corner_radius, y1), (x2 - corner_radius, y2), color, thickness)
    cv2.rectangle(overlay, (x1, y1 + corner_radius), (x2, y2 - corner_radius), color, thickness)

    # Draw four corner circles
    cv2.circle(overlay, (x1 + corner_radius, y1 + corner_radius), corner_radius, color, thickness)
    cv2.circle(overlay, (x2 - corner_radius, y1 + corner_radius), corner_radius, color, thickness)
    cv2.circle(overlay, (x1 + corner_radius, y2 - corner_radius), corner_radius, color, thickness)
    cv2.circle(overlay, (x2 - corner_radius, y2 - corner_radius), corner_radius, color, thickness)

    # Blend overlay with the original image using the alpha factor
    cv2.addWeighted(overlay, alpha, img, 1 - alpha, 0, img)
    return img


def calculate_lesion_features(mask, image, pixel_spacing, min_area_px=10):
    """
    Extracts morphological, intensity, and texture features from a binary lesion mask.
    """

    # Convert image to grayscale if it's in RGB
    if image.ndim == 3 and image.shape[2] == 3:
        image = rgb2gray(image)

    # Remove small objects from the mask and label connected regions
    bin_mask = morphology.remove_small_objects(mask.astype(bool), min_size=min_area_px)
    labeled = measure.label(bin_mask)

    # Extract properties from the first labeled region
    region = measure.regionprops(labeled, intensity_image=image)[0]

    # Morphological measurements
    area_mm2 = region.area * (pixel_spacing ** 2)  # Convert area from pixels² to mm²
    perimeter_px = perimeter_crofton(region.image, directions=4)  # Perimeter estimation
    perim_mm = perimeter_px * pixel_spacing  # Convert to mm
    circularity = (4 * np.pi * area_mm2) / (perim_mm ** 2) if perim_mm > 0 else 0.0
    eccentricity = region.eccentricity  # Shape elongation metric

    # Intensity measurements
    mean_intensity = region.mean_intensity
    std_intensity = region.intensity_std

    # Texture analysis using GLCM homogeneity
    minr, minc, maxr, maxc = region.bbox
    patch = image[minr:maxr, minc:maxc]
    mask_patch = region.image
    homogeneity = np.nan  # Default if patch too small

    if patch.size >= 4:  # Ensure patch is at least 2x2
        # Normalize patch to 0–255 range
        patch_normalized = (patch - patch.min()) / (patch.max() - patch.min() + 1e-7) * 255
        patch_normalized = patch_normalized.astype(np.uint8)

        # Quantize to 16 levels + background (17 bins)
        bins = np.linspace(0, 256, 17)
        quant = np.digitize(patch_normalized, bins) - 1
        quant = np.where(mask_patch, quant, 16).astype(np.uint8)  # Set background to level 16

        # Calculate GLCM and extract homogeneity
        glcm = feature.graycomatrix(
            quant,
            distances=[1],
            angles=[0, np.pi/4, np.pi/2, 3*np.pi/4],
            levels=17,
            symmetric=True,
            normed=True
        )
        glcm_lesion = glcm[:16, :16, :, :]
        if glcm_lesion.size and np.any(glcm_lesion):
            homogeneity = float(np.mean(feature.graycoprops(glcm_lesion, 'homogeneity')))

    # Return all features as a structured dictionary
    return {
        'morphology': {
            'area_mm2': area_mm2,
            'perimeter_mm': perim_mm,
            'circularity': circularity,
            'eccentricity': eccentricity
        },
        'intensity': {
            'mean': mean_intensity,
            'std_dev': std_intensity
        },
        'texture': {
            'glcm_homogeneity': homogeneity
        }
    }


def process_predictions(image_path: str, predictions: dict, pixel_spacing: float = None, confidence_threshold=0.5):
    """
    Main function to process model predictions:
    - Draws boxes/masks
    - Extracts lesion features
    - Encodes image regions to base64
    - Generates segmentation-only visualization
    """
    try:
        print(pixel_spacing)

        # Load image and convert to NumPy RGB array
        with Image.open(image_path) as img:
            image_np = np.array(img.convert("RGB"))

        if image_np.max() <= 1:
            image_np = (image_np * 255).astype(np.uint8)

        # Extract and filter prediction data
        boxes = torch.tensor(predictions['boxes'])
        labels = torch.tensor(predictions['labels'])
        scores = torch.tensor(predictions['scores'])
        masks = torch.tensor(predictions['masks'])
        classif = predictions['classification']

        high_conf = scores > confidence_threshold
        boxes = boxes[high_conf].numpy()
        labels = labels[high_conf].numpy()
        scores = scores[high_conf].numpy()
        masks = masks[high_conf].numpy()
        high_conf = high_conf.cpu().numpy()
        classif = [cls for cls, mask in zip(classif, high_conf) if mask]

        output_data = {'full_image': None, 'individual_predictions': []}
        full_image = image_np.copy()

        # Encode original (unmodified) image
        _, Normbuffer = cv2.imencode('.jpg', cv2.cvtColor(full_image, cv2.COLOR_RGB2BGR))

        image_height, image_width = image_np.shape[:2]
        colored_mask = np.zeros_like(image_np)

        # ── Segmentation-only image (masks on dark background) ──
        seg_image = (image_np.copy() * 0.2).astype(np.uint8)  # Darkened original

        for i, (box, label, cls_result) in enumerate(zip(boxes, labels, classif)):
            xmin, ymin, xmax, ymax = map(int, box)
            mask = (masks[i].squeeze() > 0.5).astype(np.uint8)
            print(CUSTOM_CLASSES[label - 1])

            # Assign color depending on class
            if CUSTOM_CLASSES[label - 1] == 'mass':
                color_tbm = COLOR[0]
                color_c = CONTOUR_COLOR[0]
            else:
                color_tbm = COLOR[1]
                color_c = CONTOUR_COLOR[1]

            colored_mask[:] = color_tbm

            # Create label text
            label_text = f"{int(scores[i] * 100)}% {CUSTOM_CLASSES[label - 1]}"
            font = cv2.FONT_HERSHEY_DUPLEX
            font_scale = 1.6
            thickness = 2
            (text_w, text_h), baseline = cv2.getTextSize(label_text, font, font_scale, thickness)

            # Calculate label position
            box_center = (xmin + xmax) // 2
            text_x = max(0, min(box_center - text_w // 2, image_width - text_w))
            text_y = ymin - 10
            if text_y - text_h < 0:
                text_y = ymax + text_h + 10
                if text_y + baseline > image_height:
                    text_y = ymin + (ymax - ymin) // 2

            # Draw box and label on full image
            cv2.rectangle(full_image, (xmin, ymin), (xmax, ymax), color_tbm, 2)
            padding = 5
            corner_radius = 8
            bg_top_left = (max(0, text_x - padding), max(0, text_y - text_h - padding))
            bg_bottom_right = (min(image_width, text_x + text_w + padding), min(image_height, text_y + padding))

            draw_rounded_rectangle(full_image, bg_top_left, bg_bottom_right, color_tbm, corner_radius, alpha=0.6)
            cv2.putText(full_image, label_text, (text_x, text_y), font, font_scale, (255, 255, 255), thickness)

            # Apply mask overlay
            full_image = np.where(mask[..., None],
                                  cv2.addWeighted(full_image, 1, colored_mask, 0.3, 0),
                                  full_image)

            # Draw mask contours
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            cv2.drawContours(full_image, contours, -1, color_c, 2)

            # ── Segmentation image: bright colored mask + contours on dark bg ──
            seg_colored = np.zeros_like(image_np)
            seg_colored[:] = color_tbm
            seg_image = np.where(mask[..., None],
                                 cv2.addWeighted(seg_image, 0.3, seg_colored, 0.7, 0),
                                 seg_image)
            cv2.drawContours(seg_image, contours, -1, color_c, 3)
            cv2.rectangle(seg_image, (xmin, ymin), (xmax, ymax), color_tbm, 2)
            # Add label on segmentation image
            seg_label = f"{CUSTOM_CLASSES[label - 1]} ({cls_result})"
            cv2.putText(seg_image, seg_label, (text_x, text_y), font, font_scale, (255, 255, 255), thickness)

            # Create cropped + annotated version for each prediction
            single_pred = image_np.copy()
            cv2.rectangle(single_pred, (xmin, ymin), (xmax, ymax), color_tbm, 2)
            draw_rounded_rectangle(single_pred, bg_top_left, bg_bottom_right, color_tbm, corner_radius, alpha=0.6)
            cv2.putText(single_pred, label_text, (text_x, text_y), font, font_scale, (255, 255, 255), thickness)
            single_pred = np.where(mask[..., None],
                                   cv2.addWeighted(single_pred, 1, colored_mask, 0.3, 0),
                                   single_pred)
            cv2.drawContours(single_pred, contours, -1, color_c, 2)

            # Encode single detection image
            _, buffer = cv2.imencode('.jpg', cv2.cvtColor(single_pred, cv2.COLOR_RGB2BGR))

            # Create and encode cropped region
            pad = 100
            x1 = max(0, xmin - pad)
            y1 = max(0, ymin - pad)
            x2 = min(image_width, xmax + pad)
            y2 = min(image_height, ymax + pad)
            crop = image_np[y1:y2, x1:x2]
            _, buf = cv2.imencode('.jpg', cv2.cvtColor(crop, cv2.COLOR_RGB2BGR))

            # Calculate features with NaN safety
            feat = calculate_lesion_features(mask, image_np, pixel_spacing)
            feat = sanitize_features(feat)

            # Append all outputs to the list
            output_data['individual_predictions'].append({
                'image': base64.b64encode(buffer).decode('utf-8'),
                'label': CUSTOM_CLASSES[label - 1],
                'classification': cls_result,
                'score': float(scores[i]),
                'features': feat,
                'crop': base64.b64encode(buf).decode('utf-8'),
            })

        # Final encoded full image with all detections
        _, buffer = cv2.imencode('.jpg', cv2.cvtColor(full_image, cv2.COLOR_RGB2BGR))
        output_data['full_image'] = base64.b64encode(buffer).decode('utf-8')

        # Encode segmentation image
        _, seg_buffer = cv2.imencode('.jpg', cv2.cvtColor(seg_image, cv2.COLOR_RGB2BGR))

        return {
            "full_image": output_data['full_image'],
            "detections": True,
            "full_Normal_image": base64.b64encode(Normbuffer).decode('utf-8'),
            "segmentation_image": base64.b64encode(seg_buffer).decode('utf-8'),
            "individual_predictions": output_data['individual_predictions']
        }

    except Exception as e:
        # Return error response on failure
        print(f"Error: {e}")
        return JSONResponse(
            status_code=500,
            content={"message": f"Processing error: {str(e)}"}
        )


def sanitize_features(features):
    """Replace NaN/Inf values with None for safe JSON serialization."""
    if not features:
        return features
    sanitized = {}
    for key, value in features.items():
        if isinstance(value, dict):
            sanitized[key] = sanitize_features(value)
        elif isinstance(value, float) and (np.isnan(value) or np.isinf(value)):
            sanitized[key] = None
        else:
            sanitized[key] = value
    return sanitized
