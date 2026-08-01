import torch
from torchvision.models.detection import maskrcnn_resnet50_fpn
from PIL import Image
import os
import numpy as np
import torchvision.transforms as T
import torchvision
from torchvision.models.detection import MaskRCNN
from torchvision.models.detection.backbone_utils import BackboneWithFPN
from torchvision.ops.feature_pyramid_network import LastLevelMaxPool
transform = T.Compose([
            T.ToTensor(),
        ])
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
def load_model():
    print("loading seg model ...")
    convnext = torchvision.models.convnext_tiny(weights='IMAGENET1K_V1')
    
    # 2. Identify feature layers for FPN
    return_layers = {
    '1': '0',  # First stage output (96 channels)
    '3': '1',  # Second stage output (192 channels)
    '5': '2',  # Third stage output (384 channels)
    '7': '3'   # Fourth stage output (768 channels)
    }
    
    # 3. Create feature extractor
    body = torchvision.models._utils.IntermediateLayerGetter(
        convnext.features,
        return_layers=return_layers
    )

    # 4. Configure FPN parameters
    in_channels_list = [96, 192, 384, 768]
    out_channels = 256
    
    # 5. Build BackboneWithFPN
    backbone_with_fpn = BackboneWithFPN(
        body,
        return_layers=return_layers,
        in_channels_list=in_channels_list,
        out_channels=out_channels,
        extra_blocks=LastLevelMaxPool()
    )

    # 6. Create Mask R-CNN with custom backbone
    num_classes = 4
    model = MaskRCNN(
        backbone=backbone_with_fpn,
        num_classes=num_classes,
        min_size=957,
        max_size=1147
    )
    model.rpn.anchor_generator.sizes = [
    67.5, 126.1, 191.9, 277.5, 407.9
        ]
    model.rpn.anchor_generator.aspect_ratios = [
        (0.5,1.0,2.0),
        ] * len(model.rpn.anchor_generator.sizes)

    model.load_state_dict(torch.load(os.path.join("models","MaskRcnn_bestmapkmeans.pth"), map_location=device))
    
    return model.to(device)

def predict(image_path: str, model):
    model.eval()
    merged_masks = []
    merged_labels = []
    merged_scores = []
    merged_boxes = []
    # Preprocess image
    try:
        img = Image.open(image_path).convert("RGB")

    except Exception as e:
        print(f"Error during loading from local: {e}")
        raise
    # Ensure image is on the correct device

    img_tensor = transform(img).unsqueeze(0).to(device)  # Move tensor to the correct device
    print(f"Image tensor moved to device: {device}")
    # Inference
    print(f"Performing inference on device: {device}")
    try:
        with torch.no_grad():
            prediction = model(img_tensor)
    except Exception as e:
        print(f"Error during inference: {e}")
        raise
    print('Inference completed successfully')


    try:
        confidence_threshold = 0.5
        high_conf_indices = prediction[0]['scores'] > confidence_threshold
        
        if high_conf_indices.any():
            pred_boxes = prediction[0]['boxes'][high_conf_indices].cpu().numpy()
            pred_labels = prediction[0]['labels'][high_conf_indices].cpu().numpy()
            pred_masks = prediction[0]['masks'][high_conf_indices].cpu().numpy()
            pred_scores = prediction[0]['scores'][high_conf_indices].cpu().numpy()

            # Merge only overlapping masks of same class
            merged_masks, merged_labels, merged_scores,merged_boxes = merge_overlapping_masks(
                pred_masks, pred_labels, pred_scores, iou_threshold=0.0
            )
        
        if (len(merged_boxes)>0):
        # Format results
            return {
                "boxes": merged_boxes.tolist(),
                "labels": merged_labels.tolist(),
                "scores": merged_scores.tolist(),
                "masks": merged_masks.tolist(),
            }
        return { "boxes":[],
                "labels": [],
                "scores": [],
                "masks": []}
    except Exception as e:
            print(f"Error during merging masks: {e}")
            raise

def calculate_iou(mask1, mask2):
    """Calculate Intersection over Union between two binary masks."""
    intersection = np.logical_and(mask1, mask2)
    union = np.logical_or(mask1, mask2)
    iou = np.sum(intersection) / np.sum(union)
    return iou

def mask_to_box(mask):
    """Convert binary mask to bounding box [x_min, y_min, x_max, y_max]."""
    pos = np.where(mask)
    if pos[0].size == 0 or pos[1].size == 0:
        return [0, 0, 0, 0]  # Handle empty masks
    y_min = np.min(pos[0])
    y_max = np.max(pos[0])
    x_min = np.min(pos[1])
    x_max = np.max(pos[1])
    return [x_min, y_min, x_max, y_max]

def merge_overlapping_masks(masks, labels, scores, iou_threshold=0.0):
    """
    Merge overlapping masks of the same class based on IoU threshold.
    Returns merged masks, labels, scores, and bounding boxes.
    """
    print("here")
    if len(masks) == 0:
        return np.array([]), np.array([]), np.array([]), np.array([])

    binary_masks = [(mask.squeeze() > 0.5).astype(np.float32) for mask in masks]
    binary_masks = np.stack(binary_masks)

    merged_masks = []
    merged_labels = []
    merged_scores = []
    merged_boxes = []
    used_indices = set()

    for i in range(len(binary_masks)):
        if i in used_indices:
            continue

        current_mask = binary_masks[i]
        current_label = labels[i]
        current_score = scores[i]
        merge_candidates = [i]

        for j in range(i + 1, len(binary_masks)):
            if (j not in used_indices and
                labels[j] == current_label and
                calculate_iou(current_mask, binary_masks[j]) > iou_threshold):
                merge_candidates.append(j)

        if len(merge_candidates) > 1:
            combined_mask = np.max(binary_masks[merge_candidates], axis=0)
            max_score = max(scores[merge_candidates])
        else:
            combined_mask = current_mask
            max_score = current_score

        merged_masks.append(combined_mask)
        merged_labels.append(current_label)
        merged_scores.append(max_score)
        merged_boxes.append(mask_to_box(combined_mask))
        used_indices.update(merge_candidates)

    if len(merged_masks) > 0:
        merged_masks = np.stack(merged_masks)
    else:
        merged_masks = []
    return np.array(merged_masks), np.array(merged_labels), np.array(merged_scores), np.array(merged_boxes)
