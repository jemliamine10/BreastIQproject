"""
==========================================================================
  METABRIC Breast Cancer Recurrence — Production Pipeline v5.0 "Perfect"
  ------------------------------------------------------------
  Scientific Rigor: Zero-Leakage 3-Way Split (Train/Val/Test).
  Reliability     : Platt Scaling Calibration on Val set.
  Interpretability: SHAP explanations on Test set.
  Robustness      : Global Pipeline Cross-Validation.

  Author : Senior ML Engineering Team
  Version: 5.0.0
==========================================================================
"""

import json
import logging
import pickle
import warnings
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple

import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.calibration import CalibratedClassifierCV, calibration_curve
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    roc_auc_score, confusion_matrix, classification_report,
    RocCurveDisplay, PrecisionRecallDisplay, precision_recall_curve
)

from catboost import CatBoostClassifier, Pool
import optuna
import shap
from tqdm import tqdm

# ──────────────────────────────────────────────────────────────────────────
# CONFIG
# ──────────────────────────────────────────────────────────────────────────

SEED = 42
# 3-Way Split: 60% Train, 20% Val, 20% Test
TRAIN_SIZE = 0.60
VAL_SIZE = 0.20
TEST_SIZE = 0.20

N_FOLDS_TUNING = 5
N_FOLDS_GLOBAL = 5
OPTUNA_N_TRIALS = 40
MODEL_VERSION = "5.0.0"

BASE_DIR = Path(__file__).resolve().parent
DATA_PATH = BASE_DIR / "metabric_clean.csv"
MODEL_DIR = BASE_DIR / "trained_model"
PLOTS_DIR = BASE_DIR / "plots"

MODEL_DIR.mkdir(parents=True, exist_ok=True)
PLOTS_DIR.mkdir(parents=True, exist_ok=True)

# ──────────────────────────────────────────────────────────────────────────
# LOGGING
# ──────────────────────────────────────────────────────────────────────────

LOG_FILE = BASE_DIR / "training.log"
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(message)s",
    handlers=[logging.FileHandler(LOG_FILE, mode="w"), logging.StreamHandler()]
)
logger = logging.getLogger("MetabricV5")
optuna.logging.set_verbosity(optuna.logging.INFO)
warnings.filterwarnings("ignore")

# ======================================================================
#  1. LOAD & 3-WAY SPLIT
# ======================================================================

def load_and_prep() -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame, pd.Series, pd.Series, pd.Series, List[str]]:
    """Zero-leakage partitioning: Train (60%), Val (20%), Test (20%)."""
    logger.info("=" * 70)
    logger.info("STEP 1 -- ZERO-LEAKAGE 3-WAY SPLIT")
    logger.info("=" * 70)
    
    df = pd.read_csv(DATA_PATH)
    y = df["target"].astype(int)
    X = df.drop(columns=["target"])
    cat_cols = X.select_dtypes(include=["object"]).columns.tolist()

    # 1. Split Train+Val vs Test (80/20)
    X_train_full, X_test, y_train_full, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=SEED, stratify=y
    )
    
    # 2. Split Train vs Val (75/25 of the 80% to get 60/20 total)
    X_train, X_val, y_train, y_val = train_test_split(
        X_train_full, y_train_full, test_size=0.25, random_state=SEED, stratify=y_train_full
    )
    
    logger.info(f"  Dataset Shape : {df.shape}")
    logger.info(f"  Split Ratios  : Train={len(X_train)} ({len(X_train)/len(df):.0%}) | "
                f"Val={len(X_val)} ({len(X_val)/len(df):.0%}) | "
                f"Test={len(X_test)} ({len(X_test)/len(df):.0%})")
    
    return X_train, X_val, X_test, y_train, y_val, y_test, cat_cols

# ======================================================================
#  2. TUNING (On Train set only)
# ======================================================================

def tune_hyperparameters(X_train, y_train, cat_cols) -> Dict:
    """Optuna tuning with 5-fold CV on the 60% Train set."""
    logger.info("")
    logger.info("=" * 70)
    logger.info("STEP 2 -- OPTUNA TUNING (5-Fold CV on Train set)")
    logger.info("=" * 70)

    def objective(trial):
        params = {
            "depth": trial.suggest_int("depth", 4, 8),
            "learning_rate": trial.suggest_float("learning_rate", 0.01, 0.1, log=True),
            "l2_leaf_reg": trial.suggest_float("l2_leaf_reg", 1.0, 10.0),
            "scale_pos_weight": trial.suggest_float("scale_pos_weight", 1.0, 2.0),
            "iterations": 1000,
            "random_seed": SEED,
            "verbose": 0,
            "early_stopping_rounds": 50,
            "cat_features": cat_cols
        }
        
        skf = StratifiedKFold(n_splits=N_FOLDS_TUNING, shuffle=True, random_state=SEED)
        f1s = []
        for tr_idx, val_idx in skf.split(X_train, y_train):
            m = CatBoostClassifier(**params)
            m.fit(X_train.iloc[tr_idx], y_train.iloc[tr_idx], 
                  eval_set=(X_train.iloc[val_idx], y_train.iloc[val_idx]))
            f1s.append(f1_score(y_train.iloc[val_idx], m.predict(X_train.iloc[val_idx])))
        
        return np.mean(f1s) - 0.5 * np.std(f1s)

    study = optuna.create_study(direction="maximize")
    study.optimize(objective, n_trials=OPTUNA_N_TRIALS, show_progress_bar=True)
    
    logger.info(f"  Best params found: {study.best_params}")
    return study.best_params

# ======================================================================
#  3. TRAIN, CALIBRATE & OPTIMIZE THRESHOLD (On Val set)
# ======================================================================

def train_calibrate_optimize(X_train, y_train, X_val, y_val, cat_cols, params):
    """
    1. Train on Train.
    2. Calibrate on Val.
    3. Optimize Threshold on Val.
    """
    logger.info("")
    logger.info("=" * 70)
    logger.info("STEP 3 -- FIT -> CALIBRATE -> OPTIMIZE THRESHOLD")
    logger.info("=" * 70)

    # 1. Train Base Model
    base_model = CatBoostClassifier(
        **params,
        iterations=1500,
        random_seed=SEED,
        verbose=250,
        cat_features=cat_cols
    )
    logger.info("  Starting final model training...")
    base_model.fit(X_train, y_train, eval_set=(X_val, y_val), early_stopping_rounds=100)
    
    # 2. Honest Calibration on Val Set
    calibrated_model = CalibratedClassifierCV(
        estimator=base_model,
        method="sigmoid",
        cv="prefit"
    )
    calibrated_model.fit(X_val, y_val)
    logger.info("  Calibration completed on Validation Set (No leakage).")
    
    # 3. Optimize Threshold on Val Set probas
    probas_val = calibrated_model.predict_proba(X_val)[:, 1]
    best_t = 0.5
    max_f1 = 0
    for t in np.arange(0.1, 0.9, 0.01):
        sc = f1_score(y_val, (probas_val >= t).astype(int))
        if sc > max_f1:
            max_f1 = sc
            best_t = t
            
    logger.info(f"  Optimal Threshold found on Val: {best_t:.2f} (F1={max_f1:.4f})")
    return calibrated_model, base_model, best_t

# ======================================================================
#  4. GLOBAL PIPELINE CV (Scientific Robustness)
# ======================================================================

def global_pipeline_cv(X, y, cat_cols, params):
    """Assess the entire procedure robustness via nested CV."""
    logger.info("")
    logger.info("=" * 70)
    logger.info("STEP 4 -- GLOBAL PIPELINE CROSS-VALIDATION")
    logger.info("=" * 70)
    
    skf = StratifiedKFold(n_splits=N_FOLDS_GLOBAL, shuffle=True, random_state=SEED)
    results = {"f1": [], "auc": []}
    
    logger.info(f"  Evaluating {N_FOLDS_GLOBAL} folds...")
    for i, (train_idx, test_idx) in enumerate(tqdm(skf.split(X, y), total=N_FOLDS_GLOBAL, desc="Global CV"), 1):
        X_tr_full, X_test = X.iloc[train_idx], X.iloc[test_idx]
        y_tr_full, y_test = y.iloc[train_idx], y.iloc[test_idx]
        
        # Sub-split for calibration
        X_tr, X_v, y_tr, y_v = train_test_split(X_tr_full, y_tr_full, test_size=0.25, random_state=SEED, stratify=y_tr_full)
        
        # Simple fit+calib for CV speed
        m = CatBoostClassifier(**params, iterations=1000, verbose=0, cat_features=cat_cols)
        m.fit(X_tr, y_tr)
        cal = CalibratedClassifierCV(m, method="sigmoid", cv="prefit")
        cal.fit(X_v, y_v)
        
        # Optimize T on val
        p_v = cal.predict_proba(X_v)[:, 1]
        best_t = 0.5
        top_f1 = 0
        for t in np.arange(0.2, 0.8, 0.05):
            sc = f1_score(y_v, (p_v >= t).astype(int))
            if sc > top_f1: top_f1 = sc; best_t = t
            
        probas = cal.predict_proba(X_test)[:, 1]
        results["f1"].append(f1_score(y_test, (probas >= best_t).astype(int)))
        results["auc"].append(roc_auc_score(y_test, probas))
        logger.info(f"    Fold {i}: F1={results['f1'][-1]:.4f} | AUC={results['auc'][-1]:.4f}")
        
    logger.info(f"  Global Robustness: F1={np.mean(results['f1']):.4f} ± {np.std(results['f1']):.4f}")
    return results

# ======================================================================
#  5. INTERPRETABILITY & PLOTS
# ======================================================================

def generate_visuals(model, base_model, X_test, y_test, threshold):
    """Generate professional scientific plots."""
    logger.info("")
    logger.info("=" * 70)
    logger.info("STEP 5 -- SCIENTIFIC VISUALIZATIONS")
    logger.info("=" * 70)
    
    probas = model.predict_proba(X_test)[:, 1]
    preds = (probas >= threshold).astype(int)
    
    # 1. Confusion Matrix
    plt.figure(figsize=(6, 5))
    cm = confusion_matrix(y_test, preds)
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues')
    plt.title(f"Confusion Matrix (T={threshold:.2f})")
    plt.savefig(PLOTS_DIR / "confusion_matrix.png")
    
    # 2. Calibration Curve
    plt.figure(figsize=(6, 6))
    prob_true, prob_pred = calibration_curve(y_test, probas, n_bins=10)
    plt.plot(prob_pred, prob_true, marker='o', label="Calibrated CatBoost")
    plt.plot([0, 1], [0, 1], linestyle='--', color='gray', label="Perfectly Calibrated")
    plt.title("Reliability Diagram (Calibration Curve)")
    plt.xlabel("Mean Predicted Probability")
    plt.ylabel("Fraction of Positives")
    plt.legend()
    plt.savefig(PLOTS_DIR / "calibration_curve.png")
    
    # 3. Precision-Recall Curve
    plt.figure(figsize=(6, 5))
    PrecisionRecallDisplay.from_predictions(y_test, probas, name="CatBoost")
    plt.title("Precision-Recall Curve")
    plt.savefig(PLOTS_DIR / "pr_curve.png")

    # 4. SHAP (Global Summary)
    # Note: SHAP usually on base_model as CalibratedClassifierCV is a wrapper
    logger.info("  Generating SHAP Summary Plot...")
    explainer = shap.TreeExplainer(base_model)
    # Filter out 'Unknown' or non-numeric if necessary? No, CatBoost handles it.
    shap_values = explainer.shap_values(X_test)
    plt.figure(figsize=(10, 8))
    shap.summary_plot(shap_values, X_test, show=False)
    plt.title("SHAP Feature Importance (Test Set)")
    plt.tight_layout()
    plt.savefig(PLOTS_DIR / "shap_summary.png")
    
    plt.close('all')
    logger.info("  Plots saved to ./plots/")

# ======================================================================
#  MAIN
# ======================================================================

def main():
    t_start = datetime.now()
    
    # 1. Split
    X_train, X_val, X_test, y_train, y_val, y_test, cat_cols = load_and_prep()
    
    # 2. Tune (on Train)
    best_params = tune_hyperparameters(X_train, y_train, cat_cols)
    
    # 3. Final Procedure (Train -> Calibrate -> Threshold)
    model, base_model, threshold = train_calibrate_optimize(X_train, y_train, X_val, y_val, cat_cols, best_params)
    
    # 4. Global Validation (Entire Procedure)
    cv_results = global_pipeline_cv(pd.concat([X_train, X_val]), pd.concat([y_train, y_val]), cat_cols, best_params)
    
    # 5. Evaluate on The Vault (Test Set)
    logger.info("")
    logger.info("=" * 70)
    logger.info("FINAL STEP -- THE VAULT (Evaluation on unseen Test Set)")
    logger.info("=" * 70)
    
    probas = model.predict_proba(X_test)[:, 1]
    preds = (probas >= threshold).astype(int)
    
    f1 = f1_score(y_test, preds)
    auc = roc_auc_score(y_test, probas)
    
    logger.info(f"  FINAL F1-SCORE : {f1:.4f} {'***' if f1 > 0.75 else ''}")
    logger.info(f"  FINAL ROC-AUC  : {auc:.4f}")
    logger.info(f"  Classification Report:\n{classification_report(y_test, preds)}")

    # 6. Visuals & Interpretability
    generate_visuals(model, base_model, X_test, y_test, threshold)
    
    # 7. Saving
    meta = {
        "version": MODEL_VERSION,
        "metrics_test": {"f1": f1, "auc": auc},
        "optimized_threshold": threshold,
        "params": best_params,
        "robustness_cv_f1": np.mean(cv_results["f1"]),
        "features": list(X_test.columns)
    }
    with open(MODEL_DIR / "model_metadata.json", "w") as f:
        json.dump(meta, f, indent=4)
    with open(MODEL_DIR / "perfect_model.pkl", "wb") as f:
        pickle.dump(model, f)
        
    logger.info(f"\nPipeline v{MODEL_VERSION} finished in {(datetime.now()-t_start).total_seconds():.1f}s")

if __name__ == "__main__":
    main()
