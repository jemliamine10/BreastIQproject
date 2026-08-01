# SafeScan: AI-Powered Mammogram Analysis System

This repository is part of a research project aimed at improving the detection of masses and calcifications in mammography. It contains the most effective combination of models and tools developed throughout the project.

## What’s Inside

**Detection & Segmentation:**  
Code for training and running a Mask R-CNN model to detect and segment breast abnormalities.

**Classification:**  
An SVM model that classifies the segmented regions (e.g., benign or malignant).

**Mobile User Interface:**  
A Flutter application called *SafeScan*, which allows radiologists to:

- Upload mammography images in DICOM, PNG, JPG, or JPEG formats  
- View AI-generated analysis results (detection, segmentation, classification)  
- Automatically generate diagnostic conclusions using **Qwen2.5 VL-32B Instruct** via the OpenRouter API  
- Download detailed medical reports  
- Access other integrated features designed to assist in diagnosis

## Dataset & Model Training

- The **Mask R-CNN** model was trained using the Chinese Mammography Database (CMMD).  
- The **SVM classifier** was trained using a combination of the CMMD and CBIS-DDSM datasets.  
- Dataset links (via Roboflow and TCIA) are included in the training scripts.  
- To access the trained models, submit a request using the following link:  
  [Model Access – Google Drive](https://drive.google.com/drive/folders/1Ab1ph22n9X8ICr8eg4Jdx0mJ1iVrvNmf?usp=drive_link)

## Setup Instructions

To run the project, configure the following environment files:

- `.env` for the backend (API, inference, and core logic)  
- `.env` for the frontend (Flutter app configuration)

## Note

This system is a research prototype and **not approved yet for clinical use**.

## Authors

This project was developed as a graduation research project by:  
**Noor MEZNI** and **Heithem BENMOUSSA**

## Contact

mezniinoor@gmail.com  ||  heithem.benmoussa.71@gmail.com || app.safe.scan@gmail.com
