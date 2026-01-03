"""
Games Module
Manages games, game state, and game sessions for all game types.
"""
from typing import Optional, List, Type
from fastapi import APIRouter

from app.config import settings
from app.core.game.registry import GameRegistry
from app.core.game.factory import GameFactory
from app.core.game.interfaces import GameInterface
from app.shared.container import container
from .services.game_service import GameService
from .services.session_service import GameSessionService
from .repositories.session_repository import GameSessionRepository
from .api.router import create_router
from .websocket import GameWebSocketManager, create_websocket_router, GameEventBroadcaster
from .consumers.websocket_event_consumer import GameWebSocketEventConsumer
from .events import create_event_publisher

MODULE_NAME = "games"
MODULE_DESCRIPTION = "Game engine for managing all game types and sessions"
MODULE_VERSION = "1.0.0"

# Configure which game classes to expose
# Add new games here to register them
GAME_CLASSES: List[Type[GameInterface]] = []

# Auto-import and register games
try:
    from .games.connect_four.game import ConnectFourGame

    GAME_CLASSES.append(ConnectFourGame)
except ImportError:
    pass


def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    """Setup and configure the games module."""
    if not getattr(settings, "MODULE_GAMES_ENABLED", True):
        return None

    # Register all configured games
    registry = GameRegistry()
    for game_class in GAME_CLASSES:
        registry.register(game_class)

    game_factory = GameFactory(registry)

    # Register GameService in DI container
    if not container.has(GameService):
        container.register(GameService, factory=lambda: GameService(game_factory), singleton=True)

    # Register GameSessionRepository and GameSessionService in DI container
    if not container.has(GameSessionRepository):
        container.register(GameSessionRepository, instance=GameSessionRepository(), singleton=True)
    if not container.has(GameSessionService):
        session_repository = container.get(GameSessionRepository)
        # Create event publisher for publishing game events to RabbitMQ
        event_publisher = create_event_publisher()
        container.register(
            GameSessionService,
            factory=lambda: GameSessionService(game_factory, session_repository, event_publisher),
            singleton=True
        )

    game_service = container.get(GameService)
    session_service = container.get(GameSessionService)

    # Setup and start game session consumer (for game.session.start.requested events)
    try:
        import logging
        module_logger = logging.getLogger(__name__)
        module_logger.info("Attempting to start game session consumer...")
        from .consumers.session_consumer import GameSessionEventConsumer
        session_consumer = GameSessionEventConsumer(session_service)
        module_logger.info("Game session consumer created, starting consumption...")
        session_consumer.start_consuming()
        module_logger.info("Game session consumer started successfully in module setup")
    except Exception as e:
        import logging
        module_logger = logging.getLogger(__name__)
        module_logger.error(f"Failed to start game session consumer: {e}", exc_info=True)

    # Setup WebSocket for real-time game events
    websocket_manager = GameWebSocketManager()
    event_broadcaster = GameEventBroadcaster(websocket_manager)
    websocket_event_consumer = GameWebSocketEventConsumer(websocket_manager)
    
    # Register in DI container
    if not container.has(GameWebSocketManager):
        container.register(GameWebSocketManager, instance=websocket_manager, singleton=True)
    if not container.has(GameEventBroadcaster):
        container.register(GameEventBroadcaster, instance=event_broadcaster, singleton=True)
    if not container.has(GameWebSocketEventConsumer):
        container.register(GameWebSocketEventConsumer, instance=websocket_event_consumer, singleton=True)
    
    # Start consuming RabbitMQ events for WebSocket broadcasting using the
    # new, thread-safe consumer that uses per-thread RabbitMQ clients.
    websocket_event_consumer.start_consuming()
    
    # Create routers
    rest_router = create_router(game_service, session_service)
    websocket_router = create_websocket_router(websocket_manager)
    
    # Combine routers
    combined_router = APIRouter()
    combined_router.include_router(rest_router)
    combined_router.include_router(websocket_router)
    
    return combined_router


__all__ = [
    "GameService",
    "GameSessionService",
    "setup_module",
    "GAME_CLASSES",
]
