from pydicom import dcmread
from PIL import Image
import numpy as np

def dicom_to_png(dicom_path: str, output_path: str) -> None:
    """Convert DICOM to PNG and normalization: convertion to 8bit images."""
    print("dicom {dicom_path}")
    ds = dcmread(dicom_path)
    img_array = ds.pixel_array.astype(float)
    
    # Corrected normalization line
    img_array = (np.maximum(img_array, 0) / img_array.max()) * 255.0  # Fixed
    img_array = np.uint8(img_array)
    
    if len(img_array.shape) == 2:
        img = Image.fromarray(img_array, mode='L')
    else:
        img = Image.fromarray(img_array, mode='RGB')
    print(output_path)
    img.save(output_path)