"""
Game session domain models.
"""
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Dict, Any, Optional, List


class GameSessionStatus(str, Enum):
    """Game session status enumeration."""
    CREATED = "created"
    ACTIVE = "active"
    PAUSED = "paused"
    FINISHED = "finished"
    ABANDONED = "abandoned"


@dataclass
class GameSession:
    """Domain model for game session."""

    session_id: str
    game_id: str
    game_type: str
    lobby_id: Optional[str]
    player_ids: List[str]
    current_player_id: str
    status: GameSessionStatus
    game_state: Dict[str, Any]
    started_at: datetime
    ended_at: Optional[datetime] = None
    winner_id: Optional[str] = None
    total_moves: int = 0
    game_metadata: Dict[str, Any] = None

    def __post_init__(self):
        """Initialize game_metadata if None."""
        if self.game_metadata is None:
            self.game_metadata = {}

    def is_active(self) -> bool:
        """Check if session is active."""
        return self.status == GameSessionStatus.ACTIVE

    def finish(self, winner_id: Optional[str] = None) -> None:
        """Finish the session."""
        self.status = GameSessionStatus.FINISHED
        self.winner_id = winner_id
        self.ended_at = datetime.utcnow()

    def pause(self) -> None:
        """Pause the session."""
        if self.is_active():
            self.status = GameSessionStatus.PAUSED

    def resume(self) -> None:
        """Resume the session."""
        if self.status == GameSessionStatus.PAUSED:
            self.status = GameSessionStatus.ACTIVE













