"""
Deep diagnostic analysis of the breast cancer recurrence dataset.
Goal: understand WHY F1 plateaus at ~0.60 and what's actionable.
"""
import numpy as np
import pandas as pd
from scipy.stats import chi2_contingency, pointbiserialr
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.metrics import f1_score, mutual_info_score
from catboost import CatBoostClassifier, Pool
import warnings
warnings.filterwarnings("ignore")
np.random.seed(42)

df = pd.read_csv("augmented_breast_cancer.csv", na_values=["?"])
y = (df["class"] == "recurrence-events").astype(int)
X = df.drop(columns=["class"])

print("=" * 70)
print("1. DATASET STRUCTURE")
print("=" * 70)
print(f"  Rows: {len(df)}")
print(f"  Class distribution: {dict(y.value_counts())}")
print(f"  Imbalance ratio: {y.value_counts()[0]/y.value_counts()[1]:.2f}:1")
print(f"  Missing values:")
for col in X.columns:
    n = X[col].isnull().sum()
    if n > 0:
        print(f"    {col}: {n} ({n/len(X)*100:.1f}%)")

# Fill missing for analysis
X["node-caps"] = X["node-caps"].fillna(X["node-caps"].mode()[0])
X["breast-quad"] = X["breast-quad"].fillna(X["breast-quad"].mode()[0])

print()
print("=" * 70)
print("2. INDIVIDUAL FEATURE PREDICTIVE POWER (Cramér's V / Point-Biserial)")
print("=" * 70)

def cramers_v(x, y):
    ct = pd.crosstab(x, y)
    chi2 = chi2_contingency(ct)[0]
    n = len(x)
    k = min(ct.shape) - 1
    if k == 0:
        return 0
    return np.sqrt(chi2 / (n * k))

def midpoint(s):
    try:
        parts = str(s).split("-")
        return (int(parts[0]) + int(parts[1])) / 2.0
    except:
        return np.nan

results = []
for col in X.columns:
    if X[col].dtype == "object" or col in ["deg-malig"]:
        v = cramers_v(X[col].fillna("missing"), y)
        results.append((col, "Cramér's V", v))
    else:
        corr, pval = pointbiserialr(y, X[col].fillna(0))
        results.append((col, "Point-Biserial", abs(corr)))

results.sort(key=lambda x: x[2], reverse=True)
for feat, method, score in results:
    bar = "█" * int(score * 50)
    strength = "STRONG" if score > 0.2 else ("MEDIUM" if score > 0.1 else "WEAK")
    print(f"  {feat:<15s} {method:<16s} {score:.4f}  {strength:<7s} {bar}")

print()
print("=" * 70)
print("3. FEATURE OVERLAP ANALYSIS (class separation)")
print("=" * 70)

# Convert ordinal ranges to midpoints
X_num = X.copy()
X_num["age_mid"] = X["age"].apply(midpoint)
X_num["tumor_mid"] = X["tumor-size"].apply(midpoint)
X_num["nodes_mid"] = X["inv-nodes"].apply(midpoint)

for col in ["age_mid", "tumor_mid", "nodes_mid", "deg-malig"]:
    vals = X_num[col].dropna()
    rec = vals[y == 1]
    norec = vals[y == 0]
    print(f"  {col}:")
    print(f"    No Recurrence: mean={norec.mean():.2f}, std={norec.std():.2f}")
    print(f"    Recurrence   : mean={rec.mean():.2f}, std={rec.std():.2f}")
    # Overlap = how much distributions overlap (Cohen's d)
    pooled_std = np.sqrt((norec.std()**2 + rec.std()**2) / 2)
    d = abs(rec.mean() - norec.mean()) / (pooled_std + 1e-8)
    overlap = "HIGH OVERLAP" if d < 0.5 else ("MODERATE" if d < 0.8 else "SEPARABLE")
    print(f"    Cohen's d: {d:.3f} → {overlap}")
    print()

print("=" * 70)
print("4. FEATURE REDUNDANCY (correlation between engineered features)")
print("=" * 70)

X_eng = X_num.copy()
X_eng["tumor_burden"] = X_eng["tumor_mid"] * X_eng["nodes_mid"]
X_eng["malig_tumor"] = X_eng["tumor_mid"] * X_eng["deg-malig"]
X_eng["clinical_risk"] = X_eng["deg-malig"] + (X_eng["nodes_mid"] >= 3).astype(float)*2 + (X["node-caps"]=="yes").astype(float)*1.5
X_eng["high_risk"] = (X_eng["nodes_mid"] >= 3).astype(int)
X_eng["high_malig"] = (X_eng["deg-malig"] == 3).astype(int)

eng_cols = ["tumor_mid", "nodes_mid", "deg-malig", "tumor_burden", "malig_tumor",
            "clinical_risk", "high_risk", "high_malig", "age_mid"]
corr = X_eng[eng_cols].corr()

print("  Pairs with |correlation| > 0.6:")
seen = set()
for i in range(len(eng_cols)):
    for j in range(i+1, len(eng_cols)):
        c = corr.iloc[i, j]
        if abs(c) > 0.6:
            pair = f"  {eng_cols[i]:20s} ↔ {eng_cols[j]:20s}: r={c:.3f}"
            if pair not in seen:
                print(f"    ⚠️  {pair}")
                seen.add(pair)

print()
print("=" * 70)
print("5. BAYES ERROR ESTIMATION (noisy label detection)")
print("=" * 70)

# Check: same feature values → different labels
# This tells us the NOISE FLOOR of the dataset
cat_cols_check = ["age", "menopause", "tumor-size", "inv-nodes", "node-caps", "deg-malig", "breast", "breast-quad", "irradiat"]
dup_check = df[cat_cols_check + ["class"]].copy()
grouped = dup_check.groupby(cat_cols_check)

total_groups = 0
conflicting = 0
conflicting_samples = 0

for name, group in grouped:
    total_groups += 1
    if group["class"].nunique() > 1:
        conflicting += 1
        conflicting_samples += len(group)

print(f"  Total unique feature combinations: {total_groups}")
print(f"  Conflicting groups (SAME features → DIFFERENT labels): {conflicting}")
print(f"  Samples in conflicting groups: {conflicting_samples} ({conflicting_samples/len(df)*100:.1f}%)")
print()
print(f"  >>> This means ~{conflicting_samples/len(df)*100:.0f}% of data has IDENTICAL features")
print(f"      but DIFFERENT labels → NO MODEL CAN SEPARATE THEM")
print(f"      This is the BAYES ERROR FLOOR of this dataset.")

# Estimate theoretical max accuracy
non_conflicting = len(df) - conflicting_samples
# In conflicting groups, best you can do is predict majority class
conflict_correct = 0
for name, group in grouped:
    if group["class"].nunique() > 1:
        conflict_correct += group["class"].value_counts().max()
    else:
        conflict_correct += len(group)

theoretical_acc = conflict_correct / len(df)
print(f"  Theoretical max accuracy: {theoretical_acc:.4f} ({theoretical_acc*100:.1f}%)")

print()
print("=" * 70)
print("6. CATBOOST BASELINE (raw features only, no engineering)")
print("=" * 70)

X_raw = X.copy()
cat_feats = X_raw.select_dtypes(include=["object"]).columns.tolist()

skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
f1s_raw = []
for tr, val in skf.split(X_raw, y):
    m = CatBoostClassifier(iterations=500, depth=5, learning_rate=0.05, verbose=0,
                           cat_features=cat_feats, auto_class_weights="Balanced",
                           random_seed=42)
    m.fit(Pool(X_raw.iloc[tr], y.iloc[tr], cat_features=cat_feats),
          eval_set=Pool(X_raw.iloc[val], y.iloc[val], cat_features=cat_feats))
    proba = m.predict_proba(X_raw.iloc[val])[:, 1]
    best_f1 = max(f1_score(y.iloc[val], (proba >= t).astype(int))
                  for t in np.arange(0.2, 0.7, 0.02))
    f1s_raw.append(best_f1)

print(f"  Raw features (no engineering): F1 = {np.mean(f1s_raw):.4f} ± {np.std(f1s_raw):.4f}")

print()
print("  Adding engineered features...")
X_eng2 = X_raw.copy()
X_eng2["tumor_mid"] = X["tumor-size"].apply(midpoint)
X_eng2["nodes_mid"] = X["inv-nodes"].apply(midpoint)
X_eng2["age_mid"] = X["age"].apply(midpoint)
X_eng2["tumor_burden"] = X_eng2["tumor_mid"] * X_eng2["nodes_mid"]
X_eng2["clinical_risk"] = X_eng2["deg-malig"] + (X_eng2["nodes_mid"]>=3).astype(float)*2 + (X_raw["node-caps"]=="yes").astype(float)*1.5
X_eng2["node_caps_inv"] = ((X_eng2["nodes_mid"]>=1) & (X_raw["node-caps"]=="yes")).astype(int)

cat_feats2 = X_eng2.select_dtypes(include=["object"]).columns.tolist()

f1s_eng = []
for tr, val in skf.split(X_eng2, y):
    m = CatBoostClassifier(iterations=500, depth=5, learning_rate=0.05, verbose=0,
                           cat_features=cat_feats2, auto_class_weights="Balanced",
                           random_seed=42)
    m.fit(Pool(X_eng2.iloc[tr], y.iloc[tr], cat_features=cat_feats2),
          eval_set=Pool(X_eng2.iloc[val], y.iloc[val], cat_features=cat_feats2))
    proba = m.predict_proba(X_eng2.iloc[val])[:, 1]
    best_f1 = max(f1_score(y.iloc[val], (proba >= t).astype(int))
                  for t in np.arange(0.2, 0.7, 0.02))
    f1s_eng.append(best_f1)

print(f"  With engineering: F1 = {np.mean(f1s_eng):.4f} ± {np.std(f1s_eng):.4f}")
delta = np.mean(f1s_eng) - np.mean(f1s_raw)
print(f"  Delta: {'+' if delta > 0 else ''}{delta:.4f}")
print(f"  >>> Engineering {'HELPS' if delta > 0.01 else 'DOES NOT HELP'} significantly")

print()
print("  Minimal feature set (only strong features)...")
X_min = pd.DataFrame()
X_min["deg-malig"] = X_raw["deg-malig"]
X_min["nodes_mid"] = X["inv-nodes"].apply(midpoint)
X_min["tumor_mid"] = X["tumor-size"].apply(midpoint)
X_min["node-caps"] = X_raw["node-caps"]
X_min["irradiat"] = X_raw["irradiat"]
X_min["clinical_risk"] = X_min["deg-malig"] + (X_min["nodes_mid"]>=3).astype(float)*2 + (X_min["node-caps"]=="yes").astype(float)*1.5

cat_min = ["node-caps", "irradiat"]

f1s_min = []
for tr, val in skf.split(X_min, y):
    m = CatBoostClassifier(iterations=500, depth=5, learning_rate=0.05, verbose=0,
                           cat_features=cat_min, auto_class_weights="Balanced",
                           random_seed=42)
    m.fit(Pool(X_min.iloc[tr], y.iloc[tr], cat_features=cat_min),
          eval_set=Pool(X_min.iloc[val], y.iloc[val], cat_features=cat_min))
    proba = m.predict_proba(X_min.iloc[val])[:, 1]
    best_f1 = max(f1_score(y.iloc[val], (proba >= t).astype(int))
                  for t in np.arange(0.2, 0.7, 0.02))
    f1s_min.append(best_f1)

print(f"  Minimal features: F1 = {np.mean(f1s_min):.4f} ± {np.std(f1s_min):.4f}")

print()
print("=" * 70)
print("7. AUGMENTATION QUALITY CHECK")
print("=" * 70)

# Check if augmented data is just noise
original = pd.read_csv("breast-cancer.data", header=None,
                       names=["class","age","menopause","tumor-size","inv-nodes",
                              "node-caps","deg-malig","breast","breast-quad","irradiat"],
                       na_values=["?"])
print(f"  Original dataset : {len(original)} rows")
print(f"  Augmented dataset: {len(df)} rows")
print(f"  Added rows       : {len(df) - len(original)}")
print(f"  Original class dist: {dict((original['class']=='recurrence-events').astype(int).value_counts())}")
print(f"  Augmented class dist: {dict(y.value_counts())}")

# Test: does augmentation help or hurt?
y_orig = (original["class"] == "recurrence-events").astype(int)
X_orig = original.drop(columns=["class"])
X_orig["node-caps"] = X_orig["node-caps"].fillna(X_orig["node-caps"].mode()[0])
X_orig["breast-quad"] = X_orig["breast-quad"].fillna(X_orig["breast-quad"].mode()[0])
cat_orig = X_orig.select_dtypes(include=["object"]).columns.tolist()

f1s_orig = []
skf_orig = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
for tr, val in skf_orig.split(X_orig, y_orig):
    m = CatBoostClassifier(iterations=500, depth=5, learning_rate=0.05, verbose=0,
                           cat_features=cat_orig, auto_class_weights="Balanced",
                           random_seed=42)
    m.fit(Pool(X_orig.iloc[tr], y_orig.iloc[tr], cat_features=cat_orig),
          eval_set=Pool(X_orig.iloc[val], y_orig.iloc[val], cat_features=cat_orig))
    proba = m.predict_proba(X_orig.iloc[val])[:, 1]
    best_f1 = max(f1_score(y_orig.iloc[val], (proba >= t).astype(int))
                  for t in np.arange(0.2, 0.7, 0.02))
    f1s_orig.append(best_f1)

print(f"")
print(f"  CatBoost on ORIGINAL data : F1 = {np.mean(f1s_orig):.4f} ± {np.std(f1s_orig):.4f}")
print(f"  CatBoost on AUGMENTED data: F1 = {np.mean(f1s_raw):.4f} ± {np.std(f1s_raw):.4f}")
aug_delta = np.mean(f1s_raw) - np.mean(f1s_orig)
print(f"  Delta: {'+' if aug_delta > 0 else ''}{aug_delta:.4f}")
if aug_delta < -0.01:
    print(f"  ⚠️  AUGMENTATION IS HURTING PERFORMANCE!")
    print(f"      The added rows are introducing noise.")
elif aug_delta < 0.01:
    print(f"  ⚠️  Augmentation provides NO meaningful improvement.")
else:
    print(f"  ✅  Augmentation helps.")

print()
print("=" * 70)
print("8. FINAL DIAGNOSIS")
print("=" * 70)
print(f"""
  ┌─────────────────────────────────────────────────────┐
  │ DATASET SIGNAL ANALYSIS                             │
  ├─────────────────────────────────────────────────────┤
  │ Bayes error floor  : ~{(1-theoretical_acc)*100:.0f}% error minimum            │
  │ Conflicting samples: {conflicting_samples/len(df)*100:.0f}% of data                    │
  │ Strong features    : deg-malig, inv-nodes, node-caps│
  │ Weak features      : age, breast, breast-quad       │
  │ Raw CatBoost F1    : {np.mean(f1s_raw):.4f}                         │
  │ Engineered F1      : {np.mean(f1s_eng):.4f}                         │
  │ Original data F1   : {np.mean(f1s_orig):.4f}                         │
  └─────────────────────────────────────────────────────┘
""")
