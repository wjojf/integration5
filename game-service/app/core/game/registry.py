"""
Game registry for managing game implementations.
"""
from typing import Dict, Type, Optional, List

from .interfaces import GameInterface


class GameRegistry:
    """Registry for game implementations."""
    
    def __init__(self) -> None:
        self._games: Dict[str, Type[GameInterface]] = {}
    
    def register(self, game_class: Type[GameInterface]) -> None:
        """Register a game class."""
        game_instance = game_class()
        game_type = game_instance.game_type
        self._games[game_type] = game_class
    
    def get(self, game_type: str) -> Optional[Type[GameInterface]]:
        """Get game class by type."""
        return self._games.get(game_type)
    
    def list_types(self) -> List[str]:
        """List all registered game types."""
        return list(self._games.keys())
    
    def has(self, game_type: str) -> bool:
        """Check if game type is registered."""
        return game_type in self._games

