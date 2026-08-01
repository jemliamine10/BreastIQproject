import train_pipeline
import logging
import optuna

# Override constants for faster verification
train_pipeline.OPTUNA_N_TRIALS = 2
train_pipeline.N_FOLDS_TUNING = 2
train_pipeline.N_FOLDS_GLOBAL = 2

# Force a small number of iterations if possible by monkeypatching or just letting it run
# since CatBoost verbose=250 will show up early if iterations > 250.

if __name__ == "__main__":
    print("Starting Lite Verification...")
    train_pipeline.main()
