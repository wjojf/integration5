"""
Game session event consumer.
Consumes game.session.start.requested events and creates sessions.
"""
import logging
import threading
import uuid
from typing import Dict, Any, Optional
from datetime import datetime

from app.config import settings
from app.shared.messaging import get_rabbitmq_client
from app.modules.games.services.session_service import GameSessionService

logger = logging.getLogger(__name__)


class GameSessionEventConsumer:
    """Consumer for game session lifecycle events."""

    def __init__(self, session_service: GameSessionService) -> None:
        self.session_service = session_service
        self.rabbitmq_client = get_rabbitmq_client()

        self._queue_session_start_requested = "game.session.start.requested"
        self._routing_key_session_start_requested = "game.session.start.requested"
        self._exchange_name = settings.RABBITMQ_EXCHANGE_NAME

        self._consuming: bool = False
        self._thread: Optional[threading.Thread] = None

    def _handle_session_start_requested(self, message: Dict[str, Any]) -> None:
        """Handle game session start requested event."""
        try:
            logger.info(f"Received game session start request: {message}")

            session_id = message.get("session_id")
            game_id = message.get("game_id")
            game_type = message.get("game_type")
            lobby_id = message.get("lobby_id")
            player_ids = message.get("player_ids", [])
            starting_player_id = message.get("starting_player_id")
            configuration = message.get("configuration", {})

            if not session_id or not game_type or not player_ids:
                logger.error(f"Invalid session start request: missing required fields - {message}")
                return

            # Create session
            session = self.session_service.create_session(
                session_id=session_id,
                game_id=game_id,
                game_type=game_type,
                lobby_id=lobby_id,
                player_ids=player_ids,
                starting_player_id=starting_player_id or player_ids[0] if player_ids else None,
                configuration=configuration
            )

            logger.info(f"Created game session: session_id={session_id}, game_type={game_type}")

            # Publish game.session.started event
            self._publish_session_started(session, lobby_id)

        except Exception as e:
            logger.error(f"Failed to handle session start request: {e}", exc_info=True)
            # Don't re-raise - event processing failures shouldn't break the consumer

    def _publish_session_started(self, session: Any, lobby_id: Optional[str]) -> None:
        """Publish game.session.started event."""
        try:
            from datetime import datetime

            event = {
                "eventId": str(uuid.uuid4()),
                "timestamp": (session.started_at.isoformat() + "Z"
                             if hasattr(session, 'started_at') and session.started_at
                             else datetime.utcnow().isoformat() + "Z"),
                "lobby_id": lobby_id,
                "session_id": session.session_id,
                "game_id": session.game_id,
                "game_type": session.game_type,
                "status": session.status.value if hasattr(session.status, 'value') else str(session.status),
                "game_state": session.game_state,
                "player_ids": session.player_ids,
                "current_player_id": session.current_player_id,
                "type": "GAME_SESSION_STARTED"
            }

            self.rabbitmq_client.publish_event(
                routing_key="game.session.started",
                event=event
            )

            logger.info(f"Published game.session.started event: session_id={session.session_id}")

        except Exception as e:
            logger.error(f"Failed to publish session started event: {e}", exc_info=True)

    def _consume_session_start_requests(self) -> None:
        """Consume session start request messages."""
        # Each thread needs its own RabbitMQ client instance (Pika is not thread-safe)
        from app.shared.messaging.rabbitmq_client import RabbitMQClient
        client = RabbitMQClient()
        client.connect()
        
        max_retries = 5
        retry_count = 0
        base_delay = 1.0
        
        try:
            while self._consuming and retry_count < max_retries:
                try:
                    logger.info(f"Connecting to RabbitMQ for session start requests...")
                    logger.info(f"Setting up queue consumer for queue: {self._queue_session_start_requested}, routing_key: {self._routing_key_session_start_requested}")
                    client.consume_queue(
                        queue_name=self._queue_session_start_requested,
                        routing_key=self._routing_key_session_start_requested,
                        callback=self._handle_session_start_requested,
                        auto_ack=False,
                        use_dead_letter=True
                    )
                    logger.info(f"Started consuming from queue: {self._queue_session_start_requested}")
                    # If we get here, consuming stopped normally
                    break
                except Exception as e:
                    retry_count += 1
                    logger.error(f"Error consuming session start requests (attempt {retry_count}/{max_retries}): {e}", exc_info=True)
                    
                    if retry_count < max_retries and self._consuming:
                        # Exponential backoff
                        delay = base_delay * (2 ** (retry_count - 1))
                        logger.info(f"Retrying in {delay} seconds...")
                        import time
                        time.sleep(delay)
                        
                        # Reconnect
                        try:
                            client.disconnect()
                        except:
                            pass
                        client = RabbitMQClient()
                        client.connect()
                    else:
                        logger.error(f"Failed to consume session start requests after {max_retries} attempts")
                        self._consuming = False
                        break
        finally:
            try:
                client.disconnect()
            except:
                pass

    def start_consuming(self) -> None:
        """Start consuming messages."""
        if self._consuming:
            logger.warning("Consumer already running")
            return

        self._consuming = True

        self._thread = threading.Thread(target=self._consume_session_start_requests, daemon=True)
        self._thread.start()

        logger.info("Game session event consumer started")

    def stop_consuming(self) -> None:
        """Stop consuming messages."""
        self._consuming = False

        if self._thread:
            self._thread.join(timeout=5)

        logger.info("Game session event consumer stopped")

