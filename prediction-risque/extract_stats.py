import pandas as pd
import json
import os

df = pd.read_csv('metabric_clean.csv')
stats = {}

# Categorical columns
cat_cols = df.select_dtypes(include=['object']).columns.tolist()
for col in cat_cols:
    stats[col] = df[col].unique().tolist()

# Numerical columns
num_cols = df.select_dtypes(exclude=['object']).columns.tolist()
for col in num_cols:
    stats[col] = {
        'min': float(df[col].min()),
        'max': float(df[col].max()),
        'mean': float(df[col].mean()),
        'type': str(df[col].dtype)
    }

with open('dataset_stats.json', 'w') as f:
    json.dump(stats, f, indent=4)

print("Stats saved to dataset_stats.json")
