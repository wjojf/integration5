"""
Connect Four game models.
"""
from dataclasses import dataclass
from typing import List, Dict, Any

from app.core.game.interfaces import GameStateInterface, GameMoveInterface


@dataclass
class ConnectFourState(GameStateInterface):
    """Connect Four game state."""
    board: List[List[int]]  # 0 = empty, 1 = player1, 2 = player2
    current_player_id: str
    player_ids: List[str]  # Track player IDs for mapping
    move_number: int = 0
    game_type: str = "connect_four"
    
    ROWS = 6
    COLS = 7
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert state to dictionary."""
        return {
            "board": self.board,
            "current_player_id": self.current_player_id,
            "player_ids": self.player_ids,
            "move_number": self.move_number,
            "game_type": self.game_type,
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "ConnectFourState":
        """Create state from dictionary."""
        # Support both 'current_player_id' and 'current_player' keys
        current_player = data.get("current_player_id") or data.get("current_player", "")
        return cls(
            board=data.get("board", [[0] * 7 for _ in range(6)]),
            current_player_id=current_player,
            player_ids=data.get("player_ids", []),
            move_number=data.get("move_number", 0),
            game_type=data.get("game_type", "connect_four"),
        )


@dataclass
class ConnectFourMove(GameMoveInterface):
    """Connect Four move."""
    column: int
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert move to dictionary."""
        return {"column": self.column}

