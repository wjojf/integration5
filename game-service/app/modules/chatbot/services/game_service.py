"""
Game service for fetching game and match history information.
"""
from __future__ import annotations

from typing import Dict, Any, List, Optional

from .base_service import BaseAPIService


class GameService(BaseAPIService):
    """Service for fetching game and match history data."""
    
    def get_match_history(self, user_id: str, token: str, limit: int = 20) -> Dict[str, Any]:
        """
        Get match history for a user.
        
        Returns:
            Dict with structure:
            {
                "matches": List[Dict],        # List of match records
                "count": int                  # Number of matches
            }
            
            Each match Dict has structure:
            {
                "session_id": str,            # Session ID
                "game_id": str,               # Game ID (UUID)
                "game_type": str,             # e.g., "connect_four"
                "status": str,                # "finished", "abandoned"
                "player_ids": List[str],      # List of player IDs
                "winner_id": str,             # Winner's user ID (optional, None if draw)
                "total_moves": int,           # Total number of moves
                "started_at": str,            # ISO datetime string (optional)
                "ended_at": str,              # ISO datetime string (optional)
            }
        
        Example response:
            {
                "matches": [
                    {
                        "session_id": "session-123",
                        "game_id": "550e8400-e29b-41d4-a716-446655440001",
                        "game_type": "connect_four",
                        "status": "finished",
                        "player_ids": ["user1", "user2"],
                        "winner_id": "user1",
                        "total_moves": 15,
                        "started_at": "2024-01-15T10:00:00Z",
                        "ended_at": "2024-01-15T10:05:00Z"
                    }
                ],
                "count": 1
            }
        """
        url = f"/api/v1/games/sessions/player/{user_id}/history?limit={limit}"
        data = self._get(url, token, timeout=10.0)  # Increased timeout for match history
        
        # The endpoint returns a List, but we wrap it in a dict for consistency
        if isinstance(data, list):
            return {"matches": data, "count": len(data)}
        return data
    
    def get_session(self, session_id: str, token: str) -> Optional[Dict[str, Any]]:
        """
        Get a specific game session by ID.
        
        Returns:
            Dict with structure:
            {
                "session_id": str,
                "game_id": str,
                "game_type": str,
                "status": str,                # "active", "finished", "abandoned", "paused"
                "current_player_id": str,
                "player_ids": List[str],
                "game_state": Dict[str, Any], # Game-specific state
                "total_moves": int,
                "winner_id": str              # Optional
            }
        """
        try:
            return self._get(f"/api/v1/games/sessions/{session_id}", token)
        except Exception:
            return None

