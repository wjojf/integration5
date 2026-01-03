"""Data Transfer Objects for ML Models API"""
from typing import List, Optional
from pydantic import BaseModel, Field


class BoardState(BaseModel):
    """Board state representation"""
    board: List[List[int]] = Field(..., description="6x7 board matrix")
    current_player: int = Field(..., description="Current player (1 or 2)")
    legal_moves: List[int] = Field(..., description="List of legal column indices")


class PredictPolicyRequest(BaseModel):
    """Request for policy prediction"""
    board_state: BoardState


class PredictPolicyResponse(BaseModel):
    """Response from policy prediction"""
    policy: List[float] = Field(..., description="Probability distribution over 7 columns")
    recommended_move: int = Field(..., description="Column index with highest probability")
    confidence: float = Field(..., description="Confidence in recommended move")


class PredictWinProbRequest(BaseModel):
    """Request for win probability prediction"""
    board_state: BoardState


class PredictWinProbResponse(BaseModel):
    """Response from win probability prediction"""
    win_probability: float = Field(..., description="Estimated win probability (0.0 to 1.0)")
    interpretation: str = Field(..., description="Human-readable interpretation")


class ModelInfoResponse(BaseModel):
    """Model information"""
    model_name: str
    version: str
    accuracy: Optional[float] = None
    r2_score: Optional[float] = None
    trained_on_samples: Optional[int] = None


class HealthCheckResponse(BaseModel):
    """Health check response"""
    status: str
    models_loaded: bool
    policy_model_available: bool
    winprob_model_available: bool
