"""ML Models Service - Model Loading and Prediction Logic"""
import os
import joblib
import numpy as np
from pathlib import Path
from typing import Dict, List, Tuple, Optional
import mlflow
from sklearn.preprocessing import StandardScaler

from .config import (
    POLICY_MODEL_PATH,
    WINPROB_MODEL_PATH,
    SCALER_PATH,
    MLFLOW_TRACKING_URI,
    MLFLOW_EXPERIMENT_POLICY,
    MLFLOW_EXPERIMENT_WINPROB,
)


class MLService:
    """Service for ML model predictions"""

    def __init__(self):
        self.policy_model = None
        self.winprob_model = None
        self.scaler = None
        self._load_models()
        self._setup_mlflow()

    def _setup_mlflow(self):
        """Setup MLFlow tracking"""
        mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)

    def _load_models(self):
        """Load trained models from disk"""
        try:
            if POLICY_MODEL_PATH.exists():
                self.policy_model = joblib.load(POLICY_MODEL_PATH)
                print(f"Loaded policy model from {POLICY_MODEL_PATH}")
            else:
                print(f"Warning: Policy model not found at {POLICY_MODEL_PATH}")

            if WINPROB_MODEL_PATH.exists():
                self.winprob_model = joblib.load(WINPROB_MODEL_PATH)
                print(f"Loaded win probability model from {WINPROB_MODEL_PATH}")
            else:
                print(f"Warning: Win probability model not found at {WINPROB_MODEL_PATH}")

            if SCALER_PATH.exists():
                self.scaler = joblib.load(SCALER_PATH)
                print(f"Loaded feature scaler from {SCALER_PATH}")
            else:
                print(f"Warning: Feature scaler not found at {SCALER_PATH}")

        except Exception as e:
            print(f"Error loading models: {e}")

    def _prepare_features(self, board: List[List[int]], legal_moves: List[int]) -> np.ndarray:
        """
        Prepare features from board state and legal moves

        Args:
            board: 6x7 board matrix (0=empty, 1=player1, 2=player2)
            legal_moves: List of legal column indices

        Returns:
            Feature vector of shape (49,): 42 board cells + 7 legal move flags
        """
        # Flatten board (6x7 = 42 features)
        board_flat = np.array(board).flatten()

        # Create legal moves mask (7 features: 1 if legal, 0 if not)
        legal_mask = np.zeros(7)
        for col in legal_moves:
            if 0 <= col < 7:
                legal_mask[col] = 1

        # Concatenate: 42 + 7 = 49 features
        features = np.concatenate([board_flat, legal_mask])

        return features.reshape(1, -1)  # Shape: (1, 49)

    def predict_policy(self, board: List[List[int]], legal_moves: List[int]) -> Dict:
        """
        Predict move policy (probability distribution over columns)

        Args:
            board: 6x7 board matrix
            legal_moves: List of legal column indices

        Returns:
            Dictionary with policy, recommended_move, and confidence
        """
        if self.policy_model is None:
            raise ValueError("Policy model not loaded")

        # Prepare features
        X = self._prepare_features(board, legal_moves)

        # Get probabilities for all 7 columns
        probabilities = self.policy_model.predict_proba(X)[0]  # Shape: (7,)

        # Mask out illegal moves
        legal_mask = np.zeros(7)
        for col in legal_moves:
            if 0 <= col < 7:
                legal_mask[col] = 1

        # Zero out illegal moves and renormalize
        masked_probs = probabilities * legal_mask
        if masked_probs.sum() > 0:
            masked_probs = masked_probs / masked_probs.sum()

        # Find best move
        recommended_move = int(np.argmax(masked_probs))
        confidence = float(masked_probs[recommended_move])

        return {
            "policy": masked_probs.tolist(),
            "recommended_move": recommended_move,
            "confidence": confidence
        }

    def predict_winprob(self, board: List[List[int]], legal_moves: List[int]) -> Dict:
        """
        Predict win probability for current player

        Args:
            board: 6x7 board matrix
            legal_moves: List of legal column indices

        Returns:
            Dictionary with win_probability and interpretation
        """
        if self.winprob_model is None:
            raise ValueError("Win probability model not loaded")

        # Prepare features
        X = self._prepare_features(board, legal_moves)

        # Predict
        win_prob = float(self.winprob_model.predict(X)[0])

        # Clamp to [0, 1]
        win_prob = max(0.0, min(1.0, win_prob))

        # Interpretation
        if win_prob >= 0.7:
            interpretation = "Strong winning position"
        elif win_prob >= 0.55:
            interpretation = "Slight advantage"
        elif win_prob >= 0.45:
            interpretation = "Balanced position"
        elif win_prob >= 0.3:
            interpretation = "Slight disadvantage"
        else:
            interpretation = "Losing position"

        return {
            "win_probability": win_prob,
            "interpretation": interpretation
        }

    def is_ready(self) -> bool:
        """Check if models are loaded and ready"""
        return self.policy_model is not None and self.winprob_model is not None

    def get_model_info(self) -> Dict:
        """Get information about loaded models"""
        return {
            "policy_model": {
                "loaded": self.policy_model is not None,
                "path": str(POLICY_MODEL_PATH),
                "version": "v1_fixed",
                "test_accuracy": 0.1757  # From training
            },
            "winprob_model": {
                "loaded": self.winprob_model is not None,
                "path": str(WINPROB_MODEL_PATH),
                "version": "v1_fixed",
                "test_r2": 0.037  # From training
            },
            "scaler": {
                "loaded": self.scaler is not None,
                "path": str(SCALER_PATH)
            }
        }


# Global instance
ml_service = MLService()
