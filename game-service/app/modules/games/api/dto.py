"""
Games API DTOs.
"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from uuid import UUID

from ..domain.models import GameSessionStatus


class CreateGameRequest(BaseModel):
    """Request to create a game (stateless)."""
    game_type: str = Field(..., description="Type of game (e.g., 'connect_four')")
    player_ids: List[str] = Field(..., min_length=1, description="List of player IDs")
    starting_player_id: str = Field(..., description="ID of starting player")
    configuration: Optional[Dict[str, Any]] = Field(None, description="Game configuration")


class CreateGameResponse(BaseModel):
    """Response from creating a game."""
    game_type: str
    game_state: Dict[str, Any]
    legal_moves: List[Dict[str, Any]]
    current_player_id: str
    status: str


class ApplyMoveRequest(BaseModel):
    """Request to apply a move (stateless)."""
    game_type: str
    game_state: Dict[str, Any]
    move: Dict[str, Any]
    player_id: str


class ApplyMoveResponse(BaseModel):
    """Response from applying a move."""
    game_type: str
    game_state: Dict[str, Any]
    legal_moves: List[Dict[str, Any]]
    status: str
    current_player_id: str
    winner_id: Optional[str] = None


class GetLegalMovesRequest(BaseModel):
    """Request to get legal moves."""
    game_type: str
    game_state: Dict[str, Any]
    player_id: str


class GetLegalMovesResponse(BaseModel):
    """Response with legal moves."""
    legal_moves: List[Dict[str, Any]]


class EvaluateGameRequest(BaseModel):
    """Request to evaluate game state."""
    game_type: str
    game_state: Dict[str, Any]


class EvaluateGameResponse(BaseModel):
    """Response with game evaluation."""
    status: str
    winner_id: Optional[str] = None
    is_finished: bool
    current_player_id: str


class GameInfo(BaseModel):
    """Game information model."""
    id: str = Field(..., description="Game ID (UUID)")
    title: str = Field(..., description="Game title")
    genre: str = Field(..., description="Game genre")
    image: str = Field(..., description="Game image URL")


class ListGamesResponse(BaseModel):
    """Response listing available games."""
    games: List[GameInfo]


class GetGameDetailResponse(BaseModel):
    """Response with game detail information."""
    id: str
    title: str
    genre: str
    image: str


class CreateSessionRequest(BaseModel):
    """Request to create a game session."""
    session_id: Optional[str] = Field(None, description="Optional session ID (auto-generated if not provided)")
    game_id: str = Field(..., description="Game ID")
    game_type: str = Field(..., description="Type of game")
    lobby_id: Optional[str] = Field(None, description="Lobby ID")
    player_ids: List[str] = Field(..., min_length=1, description="List of player IDs")
    starting_player_id: str = Field(..., description="Starting player ID")
    configuration: Optional[Dict[str, Any]] = Field(None, description="Game configuration")


class CreateSessionResponse(BaseModel):
    """Response from creating a session."""
    session_id: str
    game_id: str
    game_type: str
    status: str
    current_player_id: str
    player_ids: List[str]
    game_state: Dict[str, Any]
    total_moves: int


class GetSessionResponse(BaseModel):
    """Response with session data."""
    session_id: str
    game_id: str
    game_type: str
    status: str
    current_player_id: str
    player_ids: List[str]
    game_state: Dict[str, Any]
    total_moves: int
    winner_id: Optional[str] = None


class ApplySessionMoveRequest(BaseModel):
    """Request to apply a move to a session."""
    player_id: str
    move: Dict[str, Any]


class ApplySessionMoveResponse(BaseModel):
    """Response from applying a move to a session."""
    session_id: str
    game_state: Dict[str, Any]
    status: str
    current_player_id: str
    winner_id: Optional[str] = None
    total_moves: int


class AbandonSessionRequest(BaseModel):
    """Request to abandon a game session."""
    player_id: str = Field(..., description="ID of the player abandoning the session")


class AbandonSessionResponse(BaseModel):
    """Response from abandoning a session."""
    session_id: str
    status: str
    winner_id: Optional[str] = None
    message: str







