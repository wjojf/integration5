"""
Chatbot services module.
Provides service classes for fetching user context data.

Each service documents its response structure in detailed docstrings.
See models.py for response model definitions.
"""
from .base_service import BaseAPIService
from .profile_service import ProfileService
from .friend_service import FriendService
from .achievement_service import AchievementService
from .game_service import GameService
from .lobby_service import LobbyService
from .models import (
    ProfileResponse,
    FriendResponse,
    FriendRequestResponse,
    AchievementResponse,
    MatchHistoryResponse,
    LobbyResponse,
)

__all__ = [
    "BaseAPIService",
    "ProfileService",
    "FriendService",
    "AchievementService",
    "GameService",
    "LobbyService",
    "ProfileResponse",
    "FriendResponse",
    "FriendRequestResponse",
    "AchievementResponse",
    "MatchHistoryResponse",
    "LobbyResponse",
]

