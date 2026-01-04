"""
RabbitMQ event broadcaster for WebSocket.
Consumes game events from RabbitMQ and broadcasts to WebSocket connections.
"""
import logging
import threading
import asyncio
from typing import Dict, Any, Optional
from queue import Queue

from app.config import settings
from app.shared.messaging import get_rabbitmq_client
from .connection_manager import GameWebSocketManager

logger = logging.getLogger(__name__)


class GameEventBroadcaster:
    """Consumes RabbitMQ events and broadcasts to WebSocket connections.
    
    Uses unique queue names to avoid competing with platform-backend consumers.
    Messages are received via topic exchange routing keys.
    """

    def __init__(self, connection_manager: GameWebSocketManager):
        self.connection_manager = connection_manager

        # Use unique queue names for this service to avoid competing consumers
        # with platform-backend. All queues bind to the same routing keys.
        self._queue_move_applied = "game.websocket.move_applied"
        self._queue_session_started = "game.websocket.session_started"
        self._queue_session_ended = "game.websocket.session_ended"

        self._routing_key_move_applied = "game.move.applied"
        self._routing_key_session_started = "game.session.started"
        self._routing_key_session_ended = "game.session.ended"

        self._exchange_name = settings.RABBITMQ_EXCHANGE_NAME
        self._consuming = False
        self._threads: list[threading.Thread] = []

        # Queue for passing messages from sync RabbitMQ callbacks to async WebSocket
        self._message_queue: Queue = Queue()
        self._event_loop: Optional[asyncio.AbstractEventLoop] = None

    def _handle_move_applied(self, message: Dict[str, Any]) -> None:
        """Handle game move applied event - put in queue for async processing."""
        try:
            session_id = message.get("session_id")
            if not session_id:
                logger.warn(f"Received game.move.applied without session_id: {message}")
                return

            # Put message in queue for async processing
            self._message_queue.put(("move_applied", session_id, message))

        except Exception as e:
            logger.error(f"Failed to handle move applied event: {e}", exc_info=True)

    def _handle_session_started(self, message: Dict[str, Any]) -> None:
        """Handle game session started event - put in queue for async processing."""
        try:
            session_id = message.get("session_id")
            if not session_id:
                logger.warn(f"Received game.session.started without session_id: {message}")
                return

            # Put message in queue for async processing
            self._message_queue.put(("session_started", session_id, message))

        except Exception as e:
            logger.error(f"Failed to handle session started event: {e}", exc_info=True)

    def _handle_session_ended(self, message: Dict[str, Any]) -> None:
        """Handle game session ended event - put in queue for async processing."""
        try:
            session_id = message.get("session_id")
            if not session_id:
                logger.warn(f"Received game.session.ended without session_id: {message}")
                return

            # Put message in queue for async processing
            self._message_queue.put(("session_ended", session_id, message))

        except Exception as e:
            logger.error(f"Failed to handle session ended event: {e}", exc_info=True)

    async def _process_message_queue(self) -> None:
        """Process messages from queue and broadcast to WebSocket connections."""
        while self._consuming:
            try:
                # Get message from queue (with timeout to allow checking _consuming flag)
                try:
                    event_type, session_id, message = self._message_queue.get(timeout=1.0)
                except Exception:
                    # Timeout or empty queue - continue loop to check _consuming flag
                    continue

                logger.debug(f"Processing {event_type} event - session_id={session_id}")
                await self.connection_manager.broadcast_to_session(session_id, message)

            except Exception as e:
                logger.error(f"Error processing message queue: {e}", exc_info=True)

    def _consume_move_applied(self) -> None:
        """Consume move applied events."""
        try:
            # Each thread needs its own RabbitMQ client instance
            client = get_rabbitmq_client()
            client.connect()
            client.consume_queue(
                queue_name=self._queue_move_applied,
                routing_key=self._routing_key_move_applied,
                callback=self._handle_move_applied,
                auto_ack=False
            )
        except Exception as e:
            logger.error(f"Error consuming move applied events: {e}", exc_info=True)
            self._consuming = False

    def _consume_session_started(self) -> None:
        """Consume session started events."""
        try:
            # Each thread needs its own RabbitMQ client instance
            client = get_rabbitmq_client()
            client.connect()
            client.consume_queue(
                queue_name=self._queue_session_started,
                routing_key=self._routing_key_session_started,
                callback=self._handle_session_started,
                auto_ack=False
            )
        except Exception as e:
            logger.error(f"Error consuming session started events: {e}", exc_info=True)
            self._consuming = False

    def _consume_session_ended(self) -> None:
        """Consume session ended events."""
        try:
            # Each thread needs its own RabbitMQ client instance
            client = get_rabbitmq_client()
            client.connect()
            client.consume_queue(
                queue_name=self._queue_session_ended,
                routing_key=self._routing_key_session_ended,
                callback=self._handle_session_ended,
                auto_ack=False
            )
        except Exception as e:
            logger.error(f"Error consuming session ended events: {e}", exc_info=True)
            self._consuming = False

    def start_consuming(self) -> None:
        """Start consuming RabbitMQ events in background threads."""
        if self._consuming:
            logger.warning("Event broadcaster already consuming")
            return

        self._consuming = True

        # Start async message processor
        def run_async_processor():
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            self._event_loop = loop
            loop.run_until_complete(self._process_message_queue())

        processor_thread = threading.Thread(target=run_async_processor, daemon=True)
        processor_thread.start()

        # Start consuming RabbitMQ events in separate threads
        thread1 = threading.Thread(target=self._consume_move_applied, daemon=True)
        thread2 = threading.Thread(target=self._consume_session_started, daemon=True)
        thread3 = threading.Thread(target=self._consume_session_ended, daemon=True)

        thread1.start()
        thread2.start()
        thread3.start()

        self._threads = [processor_thread, thread1, thread2, thread3]

        logger.info("Game event broadcaster started consuming RabbitMQ events")

    def stop_consuming(self) -> None:
        """Stop consuming events."""
        self._consuming = False
        logger.info("Game event broadcaster stopped")
