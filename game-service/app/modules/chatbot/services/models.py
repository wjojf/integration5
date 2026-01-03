"""
Response models for chatbot services.
Documents the structure of API responses from platform backend and game service.
"""
from __future__ import annotations

from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from datetime import datetime


@dataclass
class ProfileResponse:
    """User profile response structure."""
    id: str
    username: str
    email: Optional[str] = None
    rank: Optional[str] = None  # e.g., "BRONZE", "SILVER", "GOLD"
    experience_points: Optional[int] = None
    bio: Optional[str] = None
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None  # Full response for reference


@dataclass
class FriendResponse:
    """Friend response structure."""
    id: str
    username: str
    rank: Optional[str] = None
    experience_points: Optional[int] = None
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None


@dataclass
class FriendRequestResponse:
    """Friend request response structure."""
    id: str
    sender_id: str
    receiver_id: str
    status: str  # "PENDING", "ACCEPTED", "REJECTED", "BLOCKED"
    sender_username: Optional[str] = None
    receiver_username: Optional[str] = None
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None


@dataclass
class AchievementResponse:
    """Achievement response structure."""
    id: str
    name: str
    description: Optional[str] = None
    unlocked_at: Optional[str] = None  # ISO datetime string
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None


@dataclass
class MatchHistoryResponse:
    """Match history response structure."""
    session_id: str
    game_id: str
    game_type: str  # e.g., "connect_four"
    status: str  # "finished", "abandoned"
    player_ids: List[str]
    total_moves: int
    winner_id: Optional[str] = None
    started_at: Optional[str] = None  # ISO datetime string
    ended_at: Optional[str] = None  # ISO datetime string
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None


@dataclass
class LobbyResponse:
    """Lobby response structure."""
    id: str
    host_id: str
    player_ids: List[str]
    max_players: int
    is_private: bool
    name: Optional[str] = None
    description: Optional[str] = None
    host_username: Optional[str] = None
    game_id: Optional[str] = None
    status: Optional[str] = None  # e.g., "WAITING", "STARTED"
    created_at: Optional[str] = None  # ISO datetime string
    # Additional fields may be present
    raw_data: Optional[Dict[str, Any]] = None


# Type aliases for clarity
ProfileData = Dict[str, Any]
FriendsData = List[Dict[str, Any]]
FriendRequestsData = List[Dict[str, Any]]
AchievementsData = List[Dict[str, Any]]
MatchHistoryData = List[Dict[str, Any]]
LobbyData = Dict[str, Any]

