# Guide De Soutenance Technique
## SafeScan: Partie IA (Segmentation + Classification) et Integration avec Spring Boot

Ce document est prepare pour t'aider a repondre clairement aux questions de soutenance technique sur:
- comment l'IA fonctionne
- d'ou viennent les donnees
- comment les modeles ont ete entraines
- comment l'IA est reliee a un backend Spring Boot

Important: le projet actuel contient un backend IA en FastAPI (Python). L'application Flutter appelle ce backend directement. Si ton architecture de soutenance inclut Spring Boot, il agit generalement comme orchestrateur/API gateway entre le front et FastAPI.

---

## 1. Vue globale du systeme

Pipeline global:
1. L'utilisateur envoie une mammographie (DICOM/PNG/JPG/JPEG).
2. Le backend IA (FastAPI) normalise l'image (conversion DICOM si necessaire).
3. Modele 1 (Mask R-CNN): detecte et segmente les lesions (mass, calc).
4. Modele 2 (SVM): classe chaque lesion detectee en Benign ou Malignant.
5. Post-traitement: calcul de features radiomiques (aire, circularite, intensite, texture GLCM), generation d'images annotees, encodage Base64.
6. La reponse JSON est renvoyee au client (ou a Spring Boot).
7. Endpoint de conclusion: un prompt medical est envoye a un LLM via OpenRouter pour produire un texte de conclusion.

---

## 2. Architecture logicielle actuelle dans le code

### 2.1 Backend IA (FastAPI)
Composants principaux:
- `app/main.py`: endpoints REST (`/health`, `/predict`, `/conclusion`, `/send-email`)
- `app/model_utils.py`: chargement du modele de segmentation + inference
- `app/classifier_utils.py`: extraction de features deep + classification SVM
- `app/image_processing.py`: visualisation, extraction de features lesion, reponse JSON
- `app/dicom_utils.py`: conversion DICOM vers PNG 8 bits

Modeles charges au demarrage:
- Segmentation: `app/models/MaskRcnn_bestmapkmeans.pth`
- Classification: `app/models/svm+lgbmdensenet+convnexttiny+9720+c=6+k=sigmoid+augmentation.pkl`
- Normalisation features classif: `app/models/scaler.joblib`

### 2.2 Frontend mobile (Flutter)
- Upload vers `POST /predict`
- Envoi de prompt vers `POST /conclusion`
- Parsing de la reponse JSON et affichage des resultats

---

## 3. Donnees (dataset): provenance et role

### 3.1 Segmentation (Mask R-CNN)
Source mentionnee dans le projet:
- CMMD (Chinese Mammography Database)
- Version annotee/preparee sur Roboflow (format COCO)

Indices dans les notebooks:
- Notebook `used models+training/segmentation model/training.ipynb`
- Dataset racine type: `segmentation-calc-mass-12/`
- Splits: `train/`, `valid/`, `test/`
- Annotations: `_annotations.coco.json`

Objectif segmentation:
- Detecter + segmenter les types de lesions:
  - `calc` (calcification)
  - `mass` (masse)

### 3.2 Classification (SVM)
Source mentionnee:
- CMMD + CBIS-DDSM
- Dataset fusionne et structure en classes:
  - `benign`
  - `malignant`
- Notebook: `used models+training/classification model/training.ipynb`

Dans le notebook, les images sont lues via:
- dossiers `train`, `valid`, `test`
- redimensionnement 224x224 pour l'extraction de features

---

## 4. Modele de segmentation: comment il marche

### 4.1 Architecture
Le modele est un Mask R-CNN avec backbone personnalise:
- Backbone ConvNeXt Tiny pre-entraine ImageNet
- Features multi-niveaux connectees a un FPN (Feature Pyramid Network)
- Tete detection + segmentation de Mask R-CNN
- `num_classes = 4` dans le code (fond + classes d'interet selon mapping d'annotations)

### 4.2 Ancres (anchors)
Le code fixe des tailles d'anchors:
- `[67.5, 126.1, 191.9, 277.5, 407.9]`

Ces valeurs viennent d'une optimisation K-means sur les dimensions des objets:
- notebook `anchorsize_using_kmeans.ipynb`

Interet en soutenance:
- adapter les anchors a la distribution reelle des lesions ameliore le rappel sur petites et moyennes structures.

### 4.3 Inference
Etapes dans `predict(...)`:
1. Lecture image RGB
2. Conversion en tenseur
3. Forward pass du Mask R-CNN
4. Filtrage par score (`threshold = 0.5`)
5. Fusion des masques qui se chevauchent et appartiennent a la meme classe (IoU > 0)
6. Retour des boites, labels, scores, masks

La fusion reduit les doublons de detection et rend l'affichage final plus propre.

---

## 5. Modele de classification: comment il marche

La classification ne se fait pas sur l'image complete, mais sur chaque region detectee.

### 5.1 Principe
Pour chaque bounding box issue de la segmentation:
1. Crop intelligent de la zone lesion (`smart_crop_from_box`)
2. Redimensionnement en 224x224
3. Extraction de features deep avec deux reseaux pre-entraine ImageNet:
   - DenseNet121 (sans couche de classification finale)
   - ConvNeXtTiny (sans couche de classification finale)
4. Flatten + concatenation des features
5. Standardisation avec `scaler.joblib`
6. Classification par SVM
7. Sortie:
   - `0 => Benign`
   - `1 => Malignant`

### 5.2 Pourquoi ce choix hybride?
- Les CNN pre-entraine servent d'extracteurs de representation robustes.
- Le SVM est efficace sur des vecteurs de features concatenes de grande dimension.
- Cette approche peut etre plus stable que de finetuner un seul reseau sur dataset medical limite.

---

## 6. Post-traitement medical et valeur explicative

Le module `image_processing.py` calcule des features interpretables sur chaque masque lesion:

### 6.1 Morphologie
- `area_mm2`: aire en mm2
- `perimeter_mm`: perimetre en mm
- `circularity`: $$4\pi A / P^2$$
- `eccentricity`: elongation de la lesion

### 6.2 Intensite
- moyenne
- ecart-type

### 6.3 Texture
- homogeneite GLCM (gray-level co-occurrence matrix)

Ces features sont utiles en soutenance car elles relient la prediction IA a des descripteurs radiologiques comprensibles.

Le module genere aussi:
- image originale
- image annotee (boxes + masks + labels)
- image segmentation (fond assombri + contours)
- crops individuels

Le tout est encode en Base64 dans le JSON retour.

---

## 7. Contrat API (important pour Spring Boot)

### 7.1 `POST /predict`
Type: `multipart/form-data`

Champs:
- `file`: image mammographie (`.dcm`, `.dicom`, `.png`, `.jpg`, `.jpeg`)
- `pixel_spacing`: nombre (string cote formulaire), valeur par defaut `0.1`

Validation:
- extension verifiee
- taille max: 60 MB

#### Reponse si detection(s) trouvee(s)
```json
{
  "full_image": "<base64>",
  "detections": true,
  "full_Normal_image": "<base64>",
  "segmentation_image": "<base64>",
  "individual_predictions": [
    {
      "image": "<base64>",
      "label": "mass|calc",
      "classification": "Benign|Malignant",
      "score": 0.93,
      "features": {
        "morphology": {
          "area_mm2": 34.2,
          "perimeter_mm": 29.1,
          "circularity": 0.67,
          "eccentricity": 0.55
        },
        "intensity": {
          "mean": 121.7,
          "std_dev": 15.3
        },
        "texture": {
          "glcm_homogeneity": 0.44
        }
      },
      "crop": "<base64>"
    }
  ]
}
```

#### Reponse si aucune detection
```json
{
  "status": "success",
  "detections": false,
  "full_Normal_image": "<base64>"
}
```

### 7.2 `POST /conclusion`
Body JSON:
```json
{ "prompt": "..." }
```

Retour:
```json
{ "conclusion": "texte" }
```

Note:
- si `OPENROUTER_KEY` absent, un texte fallback local est renvoye.

---

## 8. Lien avec un backend Spring Boot

### 8.1 Etat actuel
Actuellement, Flutter appelle FastAPI directement (`/predict`, `/conclusion`).

### 8.2 Architecture recommandee avec Spring Boot
Option recommandee pour soutenance entreprise:
1. Flutter -> Spring Boot
2. Spring Boot -> FastAPI IA
3. FastAPI -> Spring Boot (reponse JSON)
4. Spring Boot -> Flutter

Role de Spring Boot:
- securite (auth JWT, controle d'acces)
- centralisation des logs
- orchestration de plusieurs microservices
- persistence (resultats, historique patient, audit)
- normalisation des erreurs

### 8.3 Exemple de flux `/predict` avec Spring Boot
1. Spring Boot recoit le `MultipartFile` du front.
2. Spring Boot forward le multipart vers FastAPI (`/predict`) via `WebClient` ou `RestClient`.
3. Spring Boot recoit le JSON IA.
4. Spring Boot peut:
   - stocker metadata en base (score, classe, timestamp)
   - conserver les images en object storage (S3/MinIO)
   - retourner un DTO front-friendly.

### 8.4 Points techniques a dire en soutenance
- Latence: segmentation + classification peuvent etre couteuses CPU/GPU.
- Timeouts: configurer timeout > inference moyenne.
- Payload size: Base64 augmente la taille (~33%).
- Robustesse: fallback si pas de detection.
- Traçabilite: sauvegarder version modele + seuil + date inference.

---

## 9. Training: comment il se fait (version soutenance)

### 9.1 Segmentation
- Preparation du dataset en format COCO (images + masques)
- Data split train/valid/test
- Entrainement Mask R-CNN (ConvNeXt + FPN)
- Scheduler LR + mixed precision (selon notebook)
- Early stopping base sur `mAP@[50:95]` avec patience 15
- Selection du meilleur checkpoint (`MaskRcnn_bestmapkmeans.pth`)

Metrique cle a expliquer:
- `mAP@[50:95]`: moyenne de precision sur plusieurs seuils IoU, plus robuste qu'un seul seuil.

### 9.2 Classification
- Construction du dataset binaire benign/malignant
- Extraction de features deep (DenseNet + ConvNeXt)
- StandardScaler pour normaliser les features
- Apprentissage SVM
- Validation sur split test
- Sauvegarde:
  - modele SVM `.pkl`
  - scaler `.joblib`

---

## 10. Questions frequentes de prof (et reponses courtes)

### Q1. Pourquoi 2 etapes (segmentation puis classification)?
Reponse: la segmentation localise la lesion, puis la classification travaille sur une ROI propre. Cela ameliore l'interpretabilite et evite que le classifieur apprenne trop de bruit global.

### Q2. Pourquoi ne pas faire un seul modele end-to-end?
Reponse: en medical, separer detection et classification facilite l'audit, l'explication clinique, et la maintenance des composants.

### Q3. D'ou viennent les donnees?
Reponse: CMMD pour segmentation; CMMD + CBIS-DDSM pour classification, avec versions annotees/preparees via Roboflow et references TCIA.

### Q4. Que faites-vous pour reduire les faux positifs?
Reponse: seuil de confiance (0.5), fusion des masques chevauchants par classe, et classement final benign/malignant base sur features deep.

### Q5. Comment relier ca a Spring Boot?
Reponse: Spring Boot agit comme couche d'orchestration et de securite, appelle FastAPI en interne, persiste les resultats, puis renvoie un DTO propre au front.

---

## 11. Limites actuelles et ameliorations proposees

Limites:
- Prototype recherche (non certifie clinique)
- Dependance potentielle au domaine d'acquisition (distribution des machines/hopitaux)
- Encodage Base64 lourd en bande passante

Ameliorations:
- calibration des probabilites
- validation multicentrique externe
- suivi MLOps (monitoring drift)
- versionning modele + dataset + metriques dans registry
- remplacement Base64 par URLs securisees vers stockage objet

---

## 12. Message de conclusion pour ta soutenance

"Notre systeme SafeScan repose sur une architecture IA en deux etapes: segmentation des lesions mammaires via Mask R-CNN, puis classification des regions detectees via un SVM alimente par des features deep DenseNet et ConvNeXt. Les donnees proviennent principalement de CMMD, completees par CBIS-DDSM pour la tache de classification. Le backend FastAPI expose des endpoints standards, facilement orchestrables par un backend Spring Boot pour la securite, la persistance et la scalabilite. Le systeme fournit non seulement une prediction, mais aussi des elements explicatifs morphologiques et texturaux, ce qui renforce la valeur clinique et pedagogique de la solution."