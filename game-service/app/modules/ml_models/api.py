"""FastAPI Endpoints for ML Models"""
from fastapi import APIRouter, HTTPException

from .models import (
    PredictPolicyRequest,
    PredictPolicyResponse,
    PredictWinProbRequest,
    PredictWinProbResponse,
    ModelInfoResponse,
    HealthCheckResponse,
)
from .service import ml_service

router = APIRouter(tags=["ML Models"])


@router.get("/health", response_model=HealthCheckResponse)
async def health_check():
    """
    Check ML service health and model availability

    Returns health status and whether models are loaded and ready
    """
    is_ready = ml_service.is_ready()
    model_info = ml_service.get_model_info()

    return HealthCheckResponse(
        status="healthy" if is_ready else "degraded",
        models_loaded=is_ready,
        policy_model_available=model_info["policy_model"]["loaded"],
        winprob_model_available=model_info["winprob_model"]["loaded"]
    )


@router.get("/status")
async def get_status():
    """
    Get detailed information about loaded models

    Returns model paths, versions, and performance metrics
    """
    return ml_service.get_model_info()


@router.post("/predict/policy", response_model=PredictPolicyResponse)
async def predict_policy(request: PredictPolicyRequest):
    """
    Predict move policy (probability distribution over columns)

    Args:
        request: Contains board_state with board, current_player, and legal_moves

    Returns:
        - policy: Probability distribution over 7 columns (0.0-1.0)
        - recommended_move: Column index with highest probability (0-6)
        - confidence: Confidence in recommended move (0.0-1.0)

    Example:
        ```json
        {
            "board_state": {
                "board": [[0,0,0,0,0,0,0], [0,0,0,0,0,0,0], [0,0,0,0,0,0,0],
                          [0,0,0,0,0,0,0], [0,0,0,1,0,0,0], [0,0,2,1,0,0,0]],
                "current_player": 1,
                "legal_moves": [0,1,2,3,4,5,6]
            }
        }
        ```
    """
    if not ml_service.is_ready():
        raise HTTPException(status_code=503, detail="ML models not loaded")

    try:
        result = ml_service.predict_policy(
            board=request.board_state.board,
            legal_moves=request.board_state.legal_moves
        )

        return PredictPolicyResponse(
            policy=result["policy"],
            recommended_move=result["recommended_move"],
            confidence=result["confidence"]
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@router.post("/predict/winprob", response_model=PredictWinProbResponse)
async def predict_winprob(request: PredictWinProbRequest):
    """
    Predict win probability for current player

    Args:
        request: Contains board_state with board, current_player, and legal_moves

    Returns:
        - win_probability: Estimated chance of winning (0.0-1.0)
        - interpretation: Human-readable interpretation of the position

    Example:
        ```json
        {
            "board_state": {
                "board": [[0,0,0,0,0,0,0], [0,0,0,0,0,0,0], [0,0,0,0,0,0,0],
                          [0,0,0,0,0,0,0], [0,0,0,1,0,0,0], [0,0,2,1,0,0,0]],
                "current_player": 1,
                "legal_moves": [0,1,2,3,4,5,6]
            }
        }
        ```
    """
    if not ml_service.is_ready():
        raise HTTPException(status_code=503, detail="ML models not loaded")

    try:
        result = ml_service.predict_winprob(
            board=request.board_state.board,
            legal_moves=request.board_state.legal_moves
        )

        return PredictWinProbResponse(
            win_probability=result["win_probability"],
            interpretation=result["interpretation"]
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@router.get("/info/policy", response_model=ModelInfoResponse)
async def get_policy_model_info():
    """Get policy model information"""
    return ModelInfoResponse(
        model_name="Policy Network",
        version="v1_fixed",
        accuracy=0.1757,
        trained_on_samples=8352,
    )


@router.get("/info/winprob", response_model=ModelInfoResponse)
async def get_winprob_model_info():
    """Get win probability model information"""
    return ModelInfoResponse(
        model_name="Win Probability Network",
        version="v1_fixed",
        r2_score=0.037,
        trained_on_samples=8352,
    )
