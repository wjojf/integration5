"""
Game event consumer entry point.
Consumes game events from RabbitMQ and processes them.
"""
import logging
import signal
import sys
from typing import Optional, Any

from app.config import settings
from app.shared.messaging import get_rabbitmq_client

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

_game_logger_service: Optional[Any] = None
_game_logger_consumer: Optional[Any] = None
_session_consumer: Optional[Any] = None

try:
    from app.modules.game_logger.service import GameLoggerService
    from app.modules.game_logger.consumers.event_consumer import GameLoggerEventConsumer
    
    if settings.MODULE_GAME_LOGGER_ENABLED and settings.LOGGER_ENABLED:
        _game_logger_service = GameLoggerService()
        if hasattr(_game_logger_service, 'initialize'):
            _game_logger_service.initialize()
        logger.info("Game logger service initialized")
except ImportError as e:
    logger.warning(f"Game logger service not available: {e}")

try:
    from app.shared.container import container
    from app.modules.games.services.session_service import GameSessionService
    from app.modules.games.consumers.session_consumer import GameSessionEventConsumer
    
    if settings.MODULE_GAMES_ENABLED:
        # Ensure games module is set up (registers services in container)
        from app.modules.games import setup_module
        setup_module()  # This registers GameSessionService in container
        
        # Get GameSessionService from DI container
        if container.has(GameSessionService):
            session_service = container.get(GameSessionService)
            _session_consumer = GameSessionEventConsumer(session_service)
            logger.info("Game session consumer initialized")
        else:
            logger.warning("GameSessionService not found in container")
except ImportError as e:
    logger.warning(f"Game session consumer not available: {e}")
except Exception as e:
    logger.warning(f"Failed to initialize game session consumer: {e}", exc_info=True)


def setup_consumer() -> Optional[Any]:
    """Setup game event consumers."""
    global _session_consumer, _game_logger_consumer
    
    rabbitmq_client = get_rabbitmq_client()
    rabbitmq_client.connect()
    
    # Setup game session consumer (for game.session.start.requested events)
    if _session_consumer:
        _session_consumer.start_consuming()
        logger.info("Game session consumer started")
    
    # Setup game logger consumer
    if _game_logger_service:
        _game_logger_consumer = GameLoggerEventConsumer(
            rabbitmq_client=None,
            game_logger_service=_game_logger_service
        )
        _game_logger_consumer.start_consuming()
        logger.info("Game logger consumer started")
    
    return _session_consumer or _game_logger_consumer


def main() -> None:
    """Main entry point for consumer."""
    global _session_consumer, _game_logger_consumer
    
    logger.info("Starting game event consumer...")
    
    consumer: Optional[Any] = None
    
    def signal_handler(sig: int, frame: Any) -> None:
        """Handle shutdown signals."""
        logger.info("Shutdown signal received, stopping consumers...")
        if consumer:
            if hasattr(consumer, 'stop_consuming'):
                consumer.stop_consuming()
        if _session_consumer:
            _session_consumer.stop_consuming()
        if _game_logger_consumer:
            _game_logger_consumer.stop_consuming()
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    try:
        consumer = setup_consumer()
        if consumer:
            logger.info("Consumer setup complete, starting to consume messages...")
        else:
            logger.warning("No consumers configured")
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
    except Exception as e:
        logger.error(f"Fatal error in consumer: {e}", exc_info=True)
        sys.exit(1)
    finally:
        if consumer:
            if hasattr(consumer, 'stop_consuming'):
                consumer.stop_consuming()
        if _session_consumer:
            _session_consumer.stop_consuming()
        if _game_logger_consumer:
            _game_logger_consumer.stop_consuming()
        logger.info("Consumers stopped")


if __name__ == "__main__":
    main()
