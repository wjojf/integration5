"""
Game Logger Module - ML-Focused
Handles logging of gameplay data for ML training with expert labels.
"""
from typing import Optional
from fastapi import APIRouter

from app.config import settings
from app.shared.container import container
from .repositories.models import SessionLogModel, GameMoveModel, Base
from .repositories.game_log_repository import GameLogRepository
from .service import GameLoggerService
from .api import create_router

MODULE_NAME = "game_logger"
MODULE_DESCRIPTION = "ML-focused gameplay data logging with expert labels"
MODULE_VERSION = "2.0.0"


def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    """Setup and configure the game logger module."""
    if not getattr(settings, "MODULE_GAME_LOGGER_ENABLED", True):
        return None

    # Register repository and service in DI container
    if not container.has(GameLogRepository):
        repository = GameLogRepository()
        if getattr(settings, "LOGGER_ENABLED", True):
            repository.initialize()
        container.register(GameLogRepository, instance=repository, singleton=True)

    if not container.has(GameLoggerService):
        repository = container.get(GameLogRepository)
        service = GameLoggerService(repository=repository)
        container.register(GameLoggerService, instance=service, singleton=True)

    return create_router(container.get(GameLoggerService))


__all__ = [
    "SessionLogModel",
    "GameMoveModel",
    "Base",
    "GameLogRepository",
    "GameLoggerService",
    "setup_module",
    "MODULE_NAME",
    "MODULE_DESCRIPTION",
    "MODULE_VERSION",
]
