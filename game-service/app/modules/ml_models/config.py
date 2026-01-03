"""ML Models Configuration"""
import os
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).parent.parent.parent.parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = DATA_DIR / "models"
DATASETS_DIR = DATA_DIR / "datasets"

# Model paths
POLICY_MODEL_PATH = MODELS_DIR / "policy_model_v1_fixed.pkl"
WINPROB_MODEL_PATH = MODELS_DIR / "winprob_model_v1_fixed.pkl"
SCALER_PATH = MODELS_DIR / "feature_scaler.pkl"

# MLFlow
MLFLOW_TRACKING_URI = os.getenv("MLFLOW_TRACKING_URI", "http://localhost:5001")
MLFLOW_EXPERIMENT_POLICY = "connect4_policy"
MLFLOW_EXPERIMENT_WINPROB = "connect4_winprob"

# Model hyperparameters
POLICY_HIDDEN_LAYERS = (128, 64, 32)
WINPROB_HIDDEN_LAYERS = (128, 64, 32)
MAX_ITERATIONS = 500
LEARNING_RATE = 0.001

# Data
TRAIN_TEST_SPLIT = 0.2
RANDOM_STATE = 42

# Ensure directories exist
MODELS_DIR.mkdir(parents=True, exist_ok=True)
DATASETS_DIR.mkdir(parents=True, exist_ok=True)
