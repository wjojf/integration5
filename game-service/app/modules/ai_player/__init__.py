"""
AI Player Module
MCTS-based AI player with 3 difficulty levels (LOW, MEDIUM, HIGH).
"""
from typing import Optional
from fastapi import APIRouter

from app.shared.container import container
from .service import AIPlayerService
from .models import AIPlayerLevel, MoveResponse
from .api import create_router

# Module info
MODULE_NAME = "ai_player"
MODULE_VERSION = "1.0.0"


def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    """Setup AI player module."""
    from app.config import settings
    
    if not settings.MODULE_AI_PLAYER_ENABLED:
        return None
    
    # Create and register service
    if not container.has(AIPlayerService):
        container.register(AIPlayerService, instance=AIPlayerService(), singleton=True)
    
    return create_router(container.get(AIPlayerService))


__all__ = [
    "AIPlayerService",
    "AIPlayerLevel", 
    "MoveResponse",
    "setup_module",
]
