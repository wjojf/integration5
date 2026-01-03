"""
Friend service for fetching friend-related information.
"""
from __future__ import annotations

from typing import Dict, Any, List, Optional

from .base_service import BaseAPIService


class FriendService(BaseAPIService):
    """Service for fetching friend-related data."""
    
    def get_friends(self, token: str) -> List[Dict[str, Any]]:
        """
        Get the authenticated user's friends list.
        
        Returns:
            List[Dict] where each Dict has structure:
            {
                "id": str,                    # Friendship ID (UUID)
                "friendId": str,              # Friend's user ID
                "friendUsername": str,        # Friend's username
                "friendRank": str,            # Friend's rank: "BRONZE", "SILVER", etc.
                "friendExperiencePoints": int, # Friend's experience points
                "status": str,                # "ACCEPTED"
                # Additional fields may be present
            }
        
        Example response:
            [
                {
                    "id": "550e8400-e29b-41d4-a716-446655440001",
                    "friendId": "550e8400-e29b-41d4-a716-446655440002",
                    "friendUsername": "test2",
                    "friendRank": "BRONZE",
                    "friendExperiencePoints": 0,
                    "status": "ACCEPTED"
                }
            ]
        """
        data = self._get("/api/platform/players/friends", token)
        # Handle both list and dict responses
        if isinstance(data, list):
            return data
        return data.get("friends", []) if isinstance(data, dict) else []
    
    def get_pending_friend_requests(self, token: str) -> List[Dict[str, Any]]:
        """
        Get pending friend requests.
        
        Returns:
            List[Dict] where each Dict has structure:
            {
                "id": str,                    # Friend request ID (UUID)
                "senderId": str,              # Sender's user ID
                "receiverId": str,            # Receiver's user ID (current user)
                "senderUsername": str,        # Sender's username (optional)
                "status": str,                # "PENDING"
                "createdAt": str,             # ISO datetime string (optional)
                # Additional fields may be present
            }
        
        Example response:
            [
                {
                    "id": "550e8400-e29b-41d4-a716-446655440003",
                    "senderId": "550e8400-e29b-41d4-a716-446655440004",
                    "receiverId": "550e8400-e29b-41d4-a716-446655440001",
                    "senderUsername": "user2",
                    "status": "PENDING"
                }
            ]
        """
        data = self._get("/api/platform/friends?status=PENDING", token)
        # Handle both list and dict responses
        if isinstance(data, list):
            return data
        return data.get("requests", []) if isinstance(data, dict) else []
    
    def get_friend_list(self, user_id: str, token: str) -> List[Dict[str, Any]]:
        """Get friend list for a specific user (alias for get_friends)."""
        return self.get_friends(token)
