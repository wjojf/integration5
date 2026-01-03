"""
Thread-safe RabbitMQ consumer for game events -> WebSocket.

This consumer is intended to replace or complement the existing
`GameEventBroadcaster` without interfering with other modules.

Key differences:
- Uses a dedicated `RabbitMQClient` instance per thread (no global singleton)
- Listens to game events and forwards them to `GameWebSocketManager`
"""

import asyncio
import logging
import threading
from typing import Any, Dict, Optional, Callable
from queue import Queue, Empty

from app.config import settings
from app.shared.messaging.rabbitmq_client import RabbitMQClient
from app.modules.games.websocket.connection_manager import GameWebSocketManager

logger = logging.getLogger(__name__)


class GameWebSocketEventConsumer:
    """
    Consumes RabbitMQ game events and broadcasts them to WebSocket clients.

    This consumer is self-contained and does not use the global
    `get_rabbitmq_client` singleton, so it will not interfere with
    existing logger or session consumers.
    """

    def __init__(self, connection_manager: GameWebSocketManager) -> None:
        self.connection_manager = connection_manager

        # Queues & routing keys – intentionally separate from existing queues
        # to avoid interfering with current bindings.
        self._queue_move_applied = "game.websocket.move_applied"
        self._queue_session_started = "game.websocket.session_started"
        self._queue_session_ended = "game.websocket.session_ended"

        self._routing_key_move_applied = "game.move.applied"
        self._routing_key_session_started = "game.session.started"
        self._routing_key_session_ended = "game.session.ended"

        self._exchange_name = settings.RABBITMQ_EXCHANGE_NAME

        self._consuming: bool = False
        self._threads: list[threading.Thread] = []

        # Queue from blocking RabbitMQ callbacks -> async WebSocket processing
        self._message_queue: "Queue[tuple[str, str, Dict[str, Any]]]" = Queue()
        self._event_loop: Optional[asyncio.AbstractEventLoop] = None

    # --- Internal helpers -------------------------------------------------

    def _new_client(self) -> RabbitMQClient:
        """
        Create a new RabbitMQ client instance with its own connection/channel.
        Called per-thread to keep Pika usage thread-safe.
        """
        client = RabbitMQClient()
        client.connect()
        return client

    def _enqueue_event(self, event_type: str, session_id: Optional[str], message: Dict[str, Any]) -> None:
        """Common helper to enqueue an event for async processing."""
        if not session_id:
            logger.warning(f"Received {event_type} event without session_id: {message}")
            return

        try:
            self._message_queue.put((event_type, session_id, message))
        except Exception as e:
            logger.error(f"Failed to enqueue {event_type} event: {e}", exc_info=True)

    # --- RabbitMQ message handlers ----------------------------------------

    def _get_session_id(self, message: Dict[str, Any]) -> Optional[str]:
        """Extract session_id from message, supporting both snake_case and camelCase."""
        return message.get("session_id") or message.get("sessionId")

    def _handle_move_applied(self, message: Dict[str, Any]) -> None:
        session_id = self._get_session_id(message)
        self._enqueue_event("move_applied", session_id, message)

    def _handle_session_started(self, message: Dict[str, Any]) -> None:
        session_id = self._get_session_id(message)
        self._enqueue_event("session_started", session_id, message)

    def _handle_session_ended(self, message: Dict[str, Any]) -> None:
        session_id = self._get_session_id(message)
        self._enqueue_event("session_ended", session_id, message)

    # --- Async processing of messages -> WebSocket ------------------------

    async def _process_message_queue(self) -> None:
        """Process messages from the internal queue and broadcast to WebSockets."""
        while self._consuming:
            try:
                try:
                    event_type, session_id, message = self._message_queue.get(timeout=1.0)
                except Empty:
                    continue

                logger.debug(f"[WebSocketEvents] Processing {event_type} - session_id={session_id}")
                await self.connection_manager.broadcast_to_session(session_id, message)
            except Exception as e:
                logger.error(f"Error processing WebSocket event queue: {e}", exc_info=True)

    def _start_async_processor(self) -> threading.Thread:
        """Start the background thread that runs the async event loop."""

        def run() -> None:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            self._event_loop = loop
            try:
                loop.run_until_complete(self._process_message_queue())
            finally:
                try:
                    loop.close()
                except Exception:
                    pass

        thread = threading.Thread(target=run, daemon=True)
        thread.start()
        return thread

    # --- Per-queue consumer threads ---------------------------------------

    def _consume_queue(
        self,
        queue_name: str,
        routing_key: str,
        handler: Callable[[Dict[str, Any]], None],
    ) -> None:
        """Generic consumer loop for a single queue."""
        client: Optional[RabbitMQClient] = None
        try:
            client = self._new_client()
            client.consume_queue(
                queue_name=queue_name,
                routing_key=routing_key,
                callback=handler,
                auto_ack=False,
            )
        except Exception as e:
            logger.error(
                f"Error consuming queue '{queue_name}' with routing key '{routing_key}': {e}",
                exc_info=True,
            )
            self._consuming = False
        finally:
            if client:
                try:
                    client.disconnect()
                except Exception:
                    pass

    def _consume_move_applied(self) -> None:
        self._consume_queue(
            queue_name=self._queue_move_applied,
            routing_key=self._routing_key_move_applied,
            handler=self._handle_move_applied,
        )

    def _consume_session_started(self) -> None:
        self._consume_queue(
            queue_name=self._queue_session_started,
            routing_key=self._routing_key_session_started,
            handler=self._handle_session_started,
        )

    def _consume_session_ended(self) -> None:
        self._consume_queue(
            queue_name=self._queue_session_ended,
            routing_key=self._routing_key_session_ended,
            handler=self._handle_session_ended,
        )

    # --- Public API -------------------------------------------------------

    def start_consuming(self) -> None:
        """Start consuming RabbitMQ events in background threads."""
        if self._consuming:
            logger.warning("GameWebSocketEventConsumer already consuming")
            return

        self._consuming = True

        processor_thread = self._start_async_processor()

        thread_move = threading.Thread(target=self._consume_move_applied, daemon=True)
        thread_started = threading.Thread(target=self._consume_session_started, daemon=True)
        thread_ended = threading.Thread(target=self._consume_session_ended, daemon=True)

        thread_move.start()
        thread_started.start()
        thread_ended.start()

        self._threads = [processor_thread, thread_move, thread_started, thread_ended]

        logger.info("GameWebSocketEventConsumer started consuming RabbitMQ events for WebSocket broadcasting")

    def stop_consuming(self) -> None:
        """Stop consuming events and shut down threads."""
        self._consuming = False
        logger.info("Stopping GameWebSocketEventConsumer...")

        for thread in self._threads:
            try:
                thread.join(timeout=5)
            except Exception:
                pass

        self._threads = []
        logger.info("GameWebSocketEventConsumer stopped")


