"""
Lobby service for fetching lobby-related information.
"""
from __future__ import annotations

from typing import Dict, Any, List, Optional

from .base_service import BaseAPIService


class LobbyService(BaseAPIService):
    """Service for fetching lobby-related data."""
    
    def get_current_lobby(self, token: str) -> Optional[Dict[str, Any]]:
        """
        Get the current user's active lobby.
        
        Returns:
            Dict with structure:
            {
                "id": str,                    # Lobby ID (UUID)
                "name": str,                  # Lobby name (optional)
                "description": str,           # Lobby description (optional)
                "hostId": str,                # Host's user ID
                "hostUsername": str,          # Host's username (optional)
                "playerIds": List[str],       # List of player IDs in lobby
                "maxPlayers": int,            # Maximum players allowed
                "isPrivate": bool,            # Whether lobby is private
                "gameId": str,                # Game ID if started (optional)
                "status": str,                # "WAITING", "STARTED", etc. (optional)
                "createdAt": str,             # ISO datetime string (optional)
                # Additional fields may be present
            }
            
            Returns None if user is not in any lobby.
        
        Example response:
            {
                "id": "550e8400-e29b-41d4-a716-446655440001",
                "name": "My Lobby",
                "description": "Fun games",
                "hostId": "550e8400-e29b-41d4-a716-446655440002",
                "hostUsername": "user1",
                "playerIds": ["550e8400-e29b-41d4-a716-446655440002", "550e8400-e29b-41d4-a716-446655440003"],
                "maxPlayers": 4,
                "isPrivate": false,
                "gameId": null,
                "status": "WAITING",
                "createdAt": "2024-01-15T10:00:00Z"
            }
        """
        try:
            return self._get("/api/platform/lobbies/current", token)
        except Exception:
            # Return None if user is not in any lobby (404)
            return None
    
    def get_lobby(self, lobby_id: str, token: str) -> Optional[Dict[str, Any]]:
        """
        Get lobby details by ID.
        
        Returns:
            Same structure as get_current_lobby().
            Returns None if lobby not found.
        """
        try:
            return self._get(f"/api/platform/lobbies/{lobby_id}", token)
        except Exception:
            return None
    
    def search_lobbies(
        self, 
        token: str, 
        game_id: Optional[str] = None, 
        username: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Search for lobbies.
        
        Args:
            token: Authentication token
            game_id: Optional game ID to filter by
            username: Optional host username to filter by
        
        Returns:
            List[Dict] where each Dict has the same structure as get_current_lobby().
        
        Example response:
            [
                {
                    "id": "550e8400-e29b-41d4-a716-446655440001",
                    "name": "Public Lobby",
                    "hostId": "550e8400-e29b-41d4-a716-446655440002",
                    "hostUsername": "user1",
                    "playerIds": ["550e8400-e29b-41d4-a716-446655440002"],
                    "maxPlayers": 4,
                    "isPrivate": false
                }
            ]
        """
        params = []
        if game_id:
            params.append(f"gameId={game_id}")
        if username:
            params.append(f"username={username}")
        
        query_string = "&".join(params)
        path = f"/api/platform/lobbies/search"
        if query_string:
            path += f"?{query_string}"
        
        data = self._get(path, token)
        # Handle both list and dict responses
        if isinstance(data, list):
            return data
        return data.get("lobbies", []) if isinstance(data, dict) else []
