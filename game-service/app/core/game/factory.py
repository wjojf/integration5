"""
Game factory for creating game instances.
"""
from typing import Optional, Dict

from .interfaces import GameInterface
from .registry import GameRegistry


class GameFactory:
    """Factory for creating game instances."""

    def __init__(self, registry: GameRegistry) -> None:
        self.registry = registry

    def create_game(self, game_type: str) -> GameInterface:
        """Create a game instance by type."""
        game_class = self.registry.get(game_type)
        if not game_class:
            raise ValueError(f"Unknown game type: {game_type}")
        return game_class()

    def list_available_games(self) -> Dict[str, str]:
        """List all available games with descriptions."""
        games = {}
        for game_type in self.registry.list_types():
            game = self.create_game(game_type)
            games[game_type] = game_type  # Can be extended with descriptions
        return games















