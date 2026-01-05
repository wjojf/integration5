"""
Achievement service for fetching achievement information.
"""
from __future__ import annotations

from typing import Dict, Any, List, Optional

from .base_service import BaseAPIService


class AchievementService(BaseAPIService):
    """Service for fetching achievement data."""
    
    def get_achievements(self, user_id: str, token: str) -> List[Dict[str, Any]]:
        """
        Get achievements for a specific user.
        
        Returns:
            List[Dict] where each Dict has structure:
            {
                "id": str,                    # Achievement ID (UUID)
                "achievementId": str,         # Achievement definition ID
                "achievement": {              # Achievement details (optional)
                    "id": str,
                    "name": str,              # e.g., "First Win"
                    "description": str,        # Achievement description
                },
                "unlockedAt": str,            # ISO datetime string
                # Additional fields may be present
            }
        
        Example response:
            [
                {
                    "id": "550e8400-e29b-41d4-a716-446655440001",
                    "achievementId": "550e8400-e29b-41d4-a716-446655440002",
                    "achievement": {
                        "id": "550e8400-e29b-41d4-a716-446655440002",
                        "name": "First Win",
                        "description": "Win your first game"
                    },
                    "unlockedAt": "2024-01-15T10:30:00Z"
                }
            ]
        """
        data = self._get(f"/api/platform/achievements/users/{user_id}", token)
        # Handle both list and dict responses
        if isinstance(data, list):
            return data
        return data.get("achievements", []) if isinstance(data, dict) else []
    
    def get_all_achievements(self, token: Optional[str] = None, game_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Get all available achievements in the system.
        
        Args:
            token: Optional authentication token
            game_id: Optional game ID to filter achievements by game
        
        Returns:
            List[Dict] where each Dict has structure:
            {
                "id": str,                    # Achievement ID (UUID)
                "gameId": str,                # Game ID (UUID)
                "name": str,                  # e.g., "First Win", "Weekend Warrior"
                "description": str,            # Achievement description
                "category": str,               # e.g., "PROGRESSION", "TIME", "SKILL"
                "rarity": str,                 # e.g., "COMMON", "RARE", "EPIC"
                # Additional fields may be present
            }
        
        Example response:
            [
                {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "gameId": "660e8400-e29b-41d4-a716-446655440001",
                    "name": "First Victory",
                    "description": "Win your first game",
                    "category": "PROGRESSION",
                    "rarity": "COMMON"
                },
                {
                    "id": "550e8400-e29b-41d4-a716-446655440001",
                    "gameId": "660e8400-e29b-41d4-a716-446655440001",
                    "name": "Weekend Warrior",
                    "description": "Play 5 games during the weekend",
                    "category": "TIME",
                    "rarity": "RARE"
                }
            ]
        """
        url = "/api/platform/achievements"
        if game_id:
            url += f"?gameId={game_id}"
        data = self._get(url, token)
        # Handle both list and dict responses
        if isinstance(data, list):
            return data
        return data.get("achievements", []) if isinstance(data, dict) else []