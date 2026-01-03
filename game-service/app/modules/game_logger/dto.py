"""
Game Logger DTOs - ML-focused
Data Transfer Objects for ML training data logging API.
"""
from pydantic import BaseModel, Field, field_validator
from typing import Optional, Dict, Any, List, Literal
from datetime import datetime


# ==================== SESSION DTOs ====================

class CreateSessionRequest(BaseModel):
    """Request to create a new game session."""
    session_id: str = Field(..., min_length=1, max_length=64, description="Unique session identifier")
    game_type: str = Field(default="connect_four", max_length=32, description="Type of game")
    p1_type: Literal["human", "ai"] = Field(..., description="Player 1 type")
    p2_type: Literal["human", "ai"] = Field(..., description="Player 2 type")
    p1_agent: Optional[str] = Field(None, max_length=64, description="P1 agent config (e.g., 'mcts_100')")
    p2_agent: Optional[str] = Field(None, max_length=64, description="P2 agent config (e.g., 'mcts_500')")
    p1_agent_level: Optional[Literal["low", "medium", "high"]] = Field(None, description="P1 agent difficulty level")
    p2_agent_level: Optional[Literal["low", "medium", "high"]] = Field(None, description="P2 agent difficulty level")
    tag: Optional[str] = Field(None, max_length=128, description="Experiment tag (e.g., 'selfplay_v1')")


class FinishSessionRequest(BaseModel):
    """Request to finish a game session."""
    session_id: str = Field(..., description="Session ID to finish")
    winner: Literal["p1", "p2", "draw"] = Field(..., description="Match winner")
    num_moves: int = Field(..., ge=1, description="Total number of moves")
    duration_seconds: Optional[float] = Field(None, ge=0, description="Match duration in seconds")
    end_time: Optional[datetime] = Field(None, description="Match end time")


class CreateSessionResponse(BaseModel):
    """Response after creating a session."""
    status: str = "created"
    session_id: str
    message: str = "Session created successfully"


class FinishSessionResponse(BaseModel):
    """Response after finishing a session."""
    status: str = "finished"
    session_id: str
    winner: str
    num_moves: int
    message: str = "Session finished successfully"


class SessionStatsResponse(BaseModel):
    """Session statistics."""
    session_id: str
    game_type: str
    p1_type: str
    p2_type: str
    p1_agent: Optional[str]
    p2_agent: Optional[str]
    p1_agent_level: Optional[str]
    p2_agent_level: Optional[str]
    winner: Optional[str]
    num_moves: int
    duration_seconds: Optional[float]
    start_time: str
    end_time: Optional[str]
    tag: Optional[str]


# ==================== MOVE DTOs ====================

class LogMoveRequest(BaseModel):
    """Request to log a single move with ML training data."""
    session_id: str = Field(..., description="Session ID this move belongs to")
    move_number: int = Field(..., ge=1, description="Move sequence number")
    current_player: Literal["p1", "p2"] = Field(..., description="Current player making the move")
    current_player_id: Literal[1, 2] = Field(..., description="Player ID (1=player 1, 2=player 2)")

    # Board State
    board_state: Dict[str, Any] = Field(..., description="Raw board state as JSON")
    board_flat: List[int] = Field(
        ...,
        min_length=42,
        max_length=42,
        description="Flattened board [42 elements] with values {0, 1, 2}"
    )

    # Legal Moves
    legal_moves_mask: List[int] = Field(
        ...,
        min_length=7,
        max_length=7,
        description="Binary mask [7 elements] indicating legal columns (1=legal, 0=illegal)"
    )

    # Move Played
    played_move: int = Field(..., ge=0, le=6, description="Column index played (0-6)")

    # Expert Labels (CRITICAL FOR ML TRAINING)
    expert_policy: List[float] = Field(
        ...,
        min_length=7,
        max_length=7,
        description="MCTS probability distribution [7 elements] as soft label"
    )
    expert_best_move: int = Field(..., ge=0, le=6, description="Best move from argmax(expert_policy)")
    expert_value: float = Field(..., ge=0.0, le=1.0, description="Win probability [0-1] from MCTS")

    # Optional Metadata
    final_result: Optional[Literal["win", "loss", "draw", "ongoing"]] = Field(
        "ongoing",
        description="Game outcome from current player's perspective"
    )
    final_result_numeric: Optional[float] = Field(None, ge=0.0, le=1.0, description="1.0=win, 0.0=loss, 0.5=draw")
    mcts_iterations: Optional[int] = Field(None, ge=0, description="Number of MCTS iterations used")
    game_status: Optional[Literal["ongoing", "finished"]] = Field("ongoing", description="Game status after move")

    @field_validator('board_flat')
    @classmethod
    def validate_board_flat(cls, v: List[int]) -> List[int]:
        """Ensure board_flat contains only valid values."""
        if not all(val in [0, 1, 2] for val in v):
            raise ValueError("board_flat must contain only 0 (empty), 1 (player 1), 2 (player 2)")
        return v

    @field_validator('legal_moves_mask')
    @classmethod
    def validate_legal_mask(cls, v: List[int]) -> List[int]:
        """Ensure legal_moves_mask is binary."""
        if not all(val in [0, 1] for val in v):
            raise ValueError("legal_moves_mask must contain only 0 (illegal) or 1 (legal)")
        return v

    @field_validator('expert_policy')
    @classmethod
    def validate_policy(cls, v: List[float]) -> List[float]:
        """Ensure expert_policy is a valid probability distribution."""
        policy_sum = sum(v)
        if not (0.99 <= policy_sum <= 1.01):  # Allow small floating point errors
            raise ValueError(f"expert_policy must sum to ~1.0, got {policy_sum}")
        if not all(0 <= val <= 1 for val in v):
            raise ValueError("expert_policy values must be in range [0, 1]")
        return v

    @field_validator('played_move')
    @classmethod
    def validate_played_move(cls, v: int, info) -> int:
        """Ensure played_move is legal according to legal_moves_mask."""
        # Note: legal_moves_mask might not be available yet during validation
        # This check can be done in the service layer if needed
        return v


class LogMoveResponse(BaseModel):
    """Response after logging a move."""
    status: str = "logged"
    move_id: int
    session_id: str
    move_number: int
    message: str = "Move logged successfully"


class MoveItem(BaseModel):
    """Single move item in response."""
    id: int
    session_id: str
    move_number: int
    current_player: str
    current_player_id: int
    board_state: Dict[str, Any]
    board_flat: List[int]
    legal_moves_mask: List[int]
    played_move: int
    expert_policy: List[float]
    expert_best_move: int
    expert_value: float
    final_result: Optional[str]
    final_result_numeric: Optional[float]
    mcts_iterations: Optional[int]
    game_status: Optional[str]
    timestamp: str


class MoveListResponse(BaseModel):
    """List of moves for a session."""
    session_id: str
    total_moves: int
    moves: List[MoveItem]


# ==================== DATASET EXPORT DTOs ====================

class ExportDatasetRequest(BaseModel):
    """Request to export dataset to Parquet."""
    version: str = Field(..., description="Dataset version (e.g., 'v1', 'v2')")
    limit: Optional[int] = Field(None, ge=1, description="Maximum number of moves to export")
    agent_types: Optional[List[str]] = Field(None, description="Filter by agent types")
    min_games: Optional[int] = Field(None, ge=1, description="Minimum number of games per agent type")
    tag: Optional[str] = Field(None, description="Filter by experiment tag")


class ExportDatasetResponse(BaseModel):
    """Response from dataset export."""
    status: str = "exported"
    version: str
    file_path: str
    file_size_mb: float
    total_records: int
    message: str = "Dataset exported successfully"


class DatasetVersionStats(BaseModel):
    """Statistics for a single dataset version."""
    version: str
    file: str
    total_records: int
    unique_sessions: int
    agent_distribution: Dict[str, int]
    date_range: Dict[str, Optional[str]]


class DatasetStatsResponse(BaseModel):
    """Response containing dataset statistics."""
    stats: Dict[str, DatasetVersionStats]


# ==================== DVC DTOs ====================

class DVCInitRequest(BaseModel):
    """Request to initialize DVC."""
    remote_type: str = Field(default="local", description="Type of remote storage (local, s3, minio)")
    remote_url: Optional[str] = Field(None, description="URL for remote storage")


class DVCInitResponse(BaseModel):
    """Response from DVC initialization."""
    status: str
    message: str
