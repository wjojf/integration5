"""
Game domain events.
"""
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, Any, List, Optional


@dataclass
class GameStartedEvent:
    """Event emitted when a game starts."""
    event_id: str
    timestamp: datetime
    game_id: str
    game_type: str
    lobby_id: Optional[str]
    player_ids: List[str]
    starting_player_id: str
    game_configuration: Dict[str, Any]


@dataclass
class GameMoveResponseEvent:
    """Event emitted when a move is applied."""
    event_id: str
    timestamp: datetime
    game_id: str
    game_type: str
    player_id: str
    move: Dict[str, Any]
    new_game_state: Dict[str, Any]
    valid: bool
    game_status: str
    winner_id: Optional[str] = None
    error_message: Optional[str] = None


@dataclass
class GameEndedEvent:
    """Event emitted when a game ends."""
    event_id: str
    timestamp: datetime
    game_id: str
    game_type: str
    winner_id: Optional[str]
    game_result: str
    final_game_state: Dict[str, Any]













