"""
Profile service for fetching user profile information.
"""
from __future__ import annotations

from typing import Dict, Any, Optional

from .base_service import BaseAPIService


class ProfileService(BaseAPIService):
    """Service for fetching user profile data."""
    
    def get_profile(self, token: str) -> Dict[str, Any]:
        """
        Get the authenticated user's profile.
        
        Returns:
            Dict with structure:
            {
                "id": str,                    # User ID (UUID)
                "username": str,              # Username
                "email": str,                 # Email address (optional)
                "rank": str,                  # Rank: "BRONZE", "SILVER", "GOLD", etc.
                "experiencePoints": int,       # Total experience points
                "bio": str,                   # User bio (optional)
                # Additional fields may be present
            }
        
        Example response:
            {
                "id": "550e8400-e29b-41d4-a716-446655440001",
                "username": "user1",
                "email": "user1@example.com",
                "rank": "BRONZE",
                "experiencePoints": 0,
                "bio": "Gaming enthusiast"
            }
        """
        return self._get("/api/platform/players", token)

