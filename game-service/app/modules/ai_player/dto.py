"""
AI Player DTOs (Data Transfer Objects)
Request and Response models for the API.
"""
from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional

from .models import AIPlayerLevel


class MoveRequest(BaseModel):
    """Request for AI to make a move."""
    game_state: Dict[str, Any] = Field(..., description="Current game state with board, current_player, player_ids")
    level: AIPlayerLevel = Field(
        default=AIPlayerLevel.MEDIUM, 
        description="AI difficulty: low, medium, high, very_high"
    )
    game_type: str = Field(default="connect_four", description="Type of game")


class MoveResponse(BaseModel):
    """Response with AI's chosen move."""
    move: Dict[str, Any] = Field(..., description="Move data (e.g., {column: 3})")
    level: AIPlayerLevel = Field(..., description="Difficulty level used")
    confidence: float = Field(default=0.0, description="AI confidence (0-1)")
    thinking_time_ms: float = Field(default=0.0, description="Time taken in ms")
    iterations: int = Field(default=0, description="MCTS iterations used")


class DifficultyRequest(BaseModel):
    """Request to adjust difficulty."""
    player_win_rate: float = Field(..., ge=0.0, le=1.0, description="Player win rate")
    current_level: AIPlayerLevel = Field(..., description="Current AI level")


class DifficultyResponse(BaseModel):
    """Response with recommended level."""
    recommended_level: AIPlayerLevel
    reason: str


class LevelInfo(BaseModel):
    """Information about a difficulty level."""
    level: str
    iterations: int


class LevelsResponse(BaseModel):
    """List of available levels."""
    levels: list[LevelInfo]


# ============== Self-Play / Concurrent AI DTOs ==============

class SelfPlayRequest(BaseModel):
    """Request to run AI vs AI games."""
    num_games: int = Field(default=10, ge=1, le=1000, description="Number of games to play")
    player1_level: AIPlayerLevel = Field(default=AIPlayerLevel.MEDIUM, description="Player 1 AI level")
    player2_level: AIPlayerLevel = Field(default=AIPlayerLevel.MEDIUM, description="Player 2 AI level")
    epsilon: float = Field(default=0.1, ge=0.0, le=1.0, description="Random move probability for variety")
    log_to_db: bool = Field(default=True, description="Log games to database for training")


class GameResult(BaseModel):
    """Result of a single game."""
    game_id: str
    winner: str  # "win_p1", "win_p2", "draw"
    total_moves: int
    duration_seconds: float


class SelfPlayResponse(BaseModel):
    """Response with self-play results."""
    total_games: int
    player1_wins: int
    player2_wins: int
    draws: int
    player1_win_rate: float
    player2_win_rate: float
    avg_moves_per_game: float
    total_duration_seconds: float
    games: Optional[List[GameResult]] = None  # Individual game results (optional)


