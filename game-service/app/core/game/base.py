"""
Base game implementation with common functionality.
"""
from abc import ABC
from typing import Dict, Any, List, Optional

from .interfaces import GameInterface, GameStateInterface, GameMoveInterface


class BaseGame(GameInterface, ABC):
    """Base class for game implementations."""

    def __init__(self, game_type: str) -> None:
        self._game_type = game_type

    @property
    def game_type(self) -> str:
        """Return the game type identifier."""
        return self._game_type

    def validate_player(self, player_id: str, player_ids: List[str]) -> None:
        """Validate that player is in the game."""
        if player_id not in player_ids:
            raise ValueError(f"Player {player_id} is not in this game")

    def validate_move(self, move: GameMoveInterface, state: GameStateInterface, player_id: str) -> None:
        """Validate a move (can be overridden by subclasses)."""
        legal_moves = self.get_legal_moves(state, player_id)
        move_dict = move.to_dict()

        if not any(
            all(move_dict.get(k) == legal_move.to_dict().get(k) for k in move_dict.keys())
            for legal_move in legal_moves
        ):
            raise ValueError(f"Invalid move: {move_dict}")










