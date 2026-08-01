import pandas as pd
import numpy as np

# Load data
df = pd.read_csv('Breast Cancer METABRIC.csv')

# Categorical columns
cat_cols = [
    'ER Status', 'PR Status', 'HER2 Status', 
    'Pam50 + Claudin-low subtype', '3-Gene classifier subtype',
    'Tumor Stage', 'Neoplasm Histologic Grade',
    'Cancer Type', 'Inferred Menopausal State', 
    'Chemotherapy', 'Radio Therapy', 'Hormone Therapy',
    'Relapse Free Status'
]

print("--- CATEGORICAL DISTRIBUTIONS ---\n")
for col in cat_cols:
    if col in df.columns:
        counts = df[col].value_counts(dropna=False)
        total = len(df)
        print(f"[{col}]")
        for val, count in counts.items():
            pct = (count / total) * 100
            print(f"  - {str(val):<30} : {count:>5} ({pct:>5.1f}%)")
        print("-" * 40)

# Numerical columns
num_cols = [
    'Age at Diagnosis', 'Tumor Size', 
    'Lymph nodes examined positive', 'Nottingham prognostic index', 
    'Mutation Count', 'Relapse Free Status (Months)'
]

print("\n--- NUMERICAL STATISTICS ---\n")
stats = df[num_cols].describe().transpose()
print(stats[['mean', 'std', 'min', '50%', 'max', 'count']].to_string())

# Missingness summary
print("\n--- MISSING VALUES SUMMARY ---\n")
miss = df.isnull().sum()
miss_pct = (miss / len(df)) * 100
miss_df = pd.DataFrame({'Missing': miss, 'Percentage': miss_pct})
print(miss_df[miss_df['Missing'] > 0].sort_values(by='Missing', ascending=False).to_string())
