"""
Game interfaces for game-agnostic design.
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional


class GameStateInterface(ABC):
    """Interface for game state representations."""

    @abstractmethod
    def to_dict(self) -> Dict[str, Any]:
        """Convert state to dictionary."""
        pass

    @classmethod
    @abstractmethod
    def from_dict(cls, data: Dict[str, Any]) -> "GameStateInterface":
        """Create state from dictionary."""
        pass


class GameMoveInterface(ABC):
    """Interface for game move representations."""

    @abstractmethod
    def to_dict(self) -> Dict[str, Any]:
        """Convert move to dictionary."""
        pass


class GameInterface(ABC):
    """Interface for game implementations."""

    @property
    @abstractmethod
    def game_type(self) -> str:
        """Return the game type identifier."""
        pass

    @abstractmethod
    def create_initial_state(
        self,
        player_ids: List[str],
        starting_player_id: str,
        configuration: Optional[Dict[str, Any]] = None
    ) -> GameStateInterface:
        """Create initial game state."""
        pass

    @abstractmethod
    def apply_move(
        self,
        state: GameStateInterface,
        move: GameMoveInterface,
        player_id: str
    ) -> GameStateInterface:
        """Apply a move to the game state."""
        pass

    @abstractmethod
    def get_legal_moves(
        self,
        state: GameStateInterface,
        player_id: str
    ) -> List[GameMoveInterface]:
        """Get legal moves for a player."""
        pass

    @abstractmethod
    def get_game_status(self, state: GameStateInterface) -> str:
        """Get game status (ongoing, win_p1, win_p2, draw)."""
        pass

    @abstractmethod
    def get_winner_id(self, state: GameStateInterface) -> Optional[str]:
        """Get winner ID if game is finished."""
        pass

    @abstractmethod
    def get_current_player_id(self, state: GameStateInterface) -> str:
        """Get current player ID."""
        pass












