import pandas as pd
import numpy as np
import re

def clean_column_name(name):
    """Standardize column names: lowercase, underscores, no special chars."""
    name = name.lower().strip()
    name = re.sub(r'[^a-z0-9_]', '_', name)
    name = re.sub(r'_+', '_', name)
    return name.strip('_')

# 1. LOAD DATA
df_raw = pd.read_csv('Breast Cancer METABRIC.csv')

# 2. TARGET CREATION (ZERO LEAKAGE)
# target = 1 if recurrence OR death due to disease
# Relapse Free Status: 'Recurred', 'Not Recurred'
# Patient's Vital Status: 'Died of Disease', 'Living', 'Died of Other Causes'

df = df_raw.copy()

# Identify components for target
is_recurred = df['Relapse Free Status'] == 'Recurred'
is_died_disease = df["Patient's Vital Status"] == 'Died of Disease'

# Final binary target
df['target'] = (is_recurred | is_died_disease).astype(int)

# 3. FEATURE SELECTION (CLINICAL & BIOLOGICAL)
# Columns to KEEP (from user request + essentials)
keep_cols = [
    'Age at Diagnosis',
    'Type of Breast Surgery',
    'Cellularity',
    'Chemotherapy',
    'Pam50 + Claudin-low subtype',
    'ER status measured by IHC',
    'ER Status',
    'Neoplasm Histologic Grade',
    'HER2 status measured by SNP6',
    'HER2 Status',
    'Tumor Other Histologic Subtype',
    'Hormone Therapy',
    'Inferred Menopausal State',
    'Integrative Cluster',
    'Primary Tumor Laterality',
    'Lymph nodes examined positive',
    'Mutation Count',
    'Nottingham prognostic index',
    'PR Status',
    'Radio Therapy',
    '3-Gene classifier subtype',
    'Tumor Size',
    'Tumor Stage',
    'target'
]

# Ensure we only pick columns that actually exist
available_cols = [c for c in keep_cols if c in df.columns]
df = df[available_cols]

# 4. STANDARDIZE COLUMN NAMES
df.columns = [clean_column_name(c) for c in df.columns]

# 5. MISSING VALUES HANDLING
# Get categorical vs numerical
cat_features = df.select_dtypes(include=['object']).columns.tolist()
num_features = df.select_dtypes(exclude=['object']).columns.tolist()
if 'target' in num_features:
    num_features.remove('target')

# A. Categorical: Fill with "Unknown"
for col in cat_features:
    df[col] = df[col].astype(str).replace(['nan', 'None', 'NaN'], 'Unknown')

# B. Numerical: Fill with Median
for col in num_features:
    median_val = df[col].median()
    df[col] = df[col].fillna(median_val)

# 6. FINAL DATA CLEANING (Remove special chars in strings for CatBoost)
for col in cat_features:
    df[col] = df[col].apply(lambda x: re.sub(r'[^a-zA-Z0-9_\-\s]', '', str(x)))

# 7. SUMMARY PREPARATION
removed_cols = [c for c in df_raw.columns if clean_column_name(c) not in df.columns]
missing_summary = df_raw[keep_cols[:-1]].isnull().mean() * 100 # Original missing %

# 8. SAVE
output_file = 'metabric_clean.csv'
df.to_csv(output_file, index=False)

print(f"--- DATASET SAVED TO: {output_file} ---")
print(f"Shape: {df.shape}")
print(f"Target Distribution: {dict(df['target'].value_counts(normalize=True).round(4))}")
print(f"\nFinal Features ({len(df.columns)}):")
print(list(df.columns))

print("\nCategorical Features:")
print(cat_features)

print("\nRemoved Columns (Leakage/Identifiers/Redundant):")
# Filter out target components
leakage_list = ['patient_id', 'overall_survival_months', 'overall_survival_status', 
                'relapse_free_status_months', 'relapse_free_status', 'patient_s_vital_status']
print([c for c in removed_cols if any(l in c.lower() for l in leakage_list)])
