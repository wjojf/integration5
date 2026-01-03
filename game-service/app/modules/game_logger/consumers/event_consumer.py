"""
Game logger event consumer (game-agnostic).
Listens to game events and logs them to database.
"""
import logging
import threading
from datetime import datetime
from typing import Dict, Any, Optional

from app.config import settings
from app.shared.messaging import get_rabbitmq_client
from app.modules.games.domain.events import (
    GameStartedEvent,
    GameMoveResponseEvent,
    GameEndedEvent,
)
from ..service import GameLoggerService

logger = logging.getLogger(__name__)


class GameLoggerEventConsumer:
    """Consumer for game logger events (game-agnostic)."""

    def __init__(self, rabbitmq_client: Any, game_logger_service: GameLoggerService) -> None:
        self.rabbitmq_client = rabbitmq_client
        self.game_logger_service = game_logger_service

        self._queue_session_started = "game_logger.session_started"
        self._queue_move_responses = "game_logger.move_responses"
        self._queue_session_ended = "game_logger.session_ended"

        self._routing_key_session_started = "game.session.started"
        self._routing_key_move_response = "game.move.response"
        self._routing_key_session_ended = "game.session.ended"

        self._consuming: bool = False
        self._threads: list[threading.Thread] = []

    def _parse_timestamp(self, timestamp_str: Any) -> datetime:
        """Parse timestamp from various formats."""
        if isinstance(timestamp_str, str):
            try:
                return datetime.fromisoformat(timestamp_str.replace('Z', '+00:00'))
            except ValueError:
                return datetime.utcnow()
        return timestamp_str if isinstance(timestamp_str, datetime) else datetime.utcnow()

    def _dict_to_game_started_event(self, data: Dict[str, Any]) -> GameStartedEvent:
        """Convert dictionary to GameStartedEvent."""
        return GameStartedEvent(
            event_id=data.get('eventId', ''),
            game_id=data.get('gameId', ''),
            game_type=data.get('gameType', ''),
            player_ids=data.get('playerIds', []),
            timestamp=self._parse_timestamp(data.get('timestamp')),
        )

    def _dict_to_move_response_event(self, data: Dict[str, Any]) -> GameMoveResponseEvent:
        """Convert dictionary to GameMoveResponseEvent."""
        return GameMoveResponseEvent(
            event_id=data.get('eventId', ''),
            game_id=data.get('gameId', ''),
            game_type=data.get('gameType', ''),
            player_id=data.get('playerId', ''),
            move=data.get('move', {}),
            new_game_state=data.get('newGameState', {}),
            game_status=data.get('gameStatus', 'ongoing'),
            timestamp=self._parse_timestamp(data.get('timestamp')),
        )

    def _dict_to_game_ended_event(self, data: Dict[str, Any]) -> GameEndedEvent:
        """Convert dictionary to GameEndedEvent."""
        return GameEndedEvent(
            event_id=data.get('eventId', ''),
            game_id=data.get('gameId', ''),
            game_type=data.get('gameType', ''),
            final_game_state=data.get('finalGameState', {}),
            game_result=data.get('gameResult', ''),
            timestamp=self._parse_timestamp(data.get('timestamp')),
        )

    def _handle_game_start(self, message: Dict[str, Any]) -> None:
        """Handle game start message."""
        try:
            event = self._dict_to_game_started_event(message)
            self._handle_game_start_event(event)
        except Exception as e:
            logger.error(f"Failed to handle game start: {e}", exc_info=True)

    def _handle_game_start_event(self, event: GameStartedEvent) -> None:
        """Handle game start event."""
        try:
            if len(event.player_ids) >= 2:
                self.game_logger_service.create_game_session(
                    game_id=event.game_id,
                    game_type=event.game_type,
                    player1_id=event.player_ids[0] if len(event.player_ids) > 0 else "player1",
                    player2_id=event.player_ids[1] if len(event.player_ids) > 1 else "player2",
                    player1_type="human",
                    player2_type="human",
                )
                logger.info(f"Created game session: game_id={event.game_id}, game_type={event.game_type}")
        except Exception as e:
            logger.error(f"Failed to create game session: {e}", exc_info=True)

    def _handle_move_response(self, message: Dict[str, Any]) -> None:
        """Handle move response message."""
        try:
            event = self._dict_to_move_response_event(message)
            self._handle_move_response_event(event)
        except Exception as e:
            logger.error(f"Failed to handle move response: {e}", exc_info=True)

    def _handle_move_response_event(self, event: GameMoveResponseEvent) -> None:
        """Handle move response event."""
        try:
            game_id = event.game_id
            game_type = event.game_type
            state_after = event.new_game_state

            move_data = event.move
            if not move_data:
                logger.warning(f"No move data: game_id={game_id}")
                return

            move_index = state_after.get('move_number', 0) if isinstance(state_after, dict) else 0

            game_logs = self.game_logger_service.get_game_logs(game_id)

            state_before: Dict[str, Any]
            if game_logs and len(game_logs) > 0:
                last_log = game_logs[-1]
                state_before = last_log.state_after if hasattr(last_log, 'state_after') else {}
            else:
                state_before = state_after.copy()
                if isinstance(state_before, dict):
                    state_before['move_number'] = 0
                logger.debug(f"No previous moves found for game: game_id={game_id}, using initial state")

            self.game_logger_service.log_move(
                game_id=game_id,
                game_type=game_type,
                move_index=move_index,
                player_id=event.player_id,
                agent_type="human",
                state_before=state_before,
                move_data=move_data,
                state_after=state_after,
                result=event.game_status,
            )

            logger.info(f"Logged move: game_id={game_id}, game_type={game_type}, move_index={move_index}")

        except Exception as e:
            logger.error(f"Failed to log move: {e}", exc_info=True)

    def _handle_game_end(self, message: Dict[str, Any]) -> None:
        """Handle game end message."""
        try:
            event = self._dict_to_game_ended_event(message)

            final_state = event.final_game_state
            total_moves = final_state.get('move_number', 0) if isinstance(final_state, dict) else 0

            self.game_logger_service.finish_game_session(
                game_id=event.game_id,
                final_result=event.game_result,
                total_moves=total_moves,
            )

            logger.info(f"Finished game session: game_id={event.game_id}, game_type={event.game_type}")

        except Exception as e:
            logger.error(f"Failed to finish game session: {e}", exc_info=True)

    def _consume_move_responses(self) -> None:
        """Consume move response messages."""
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
                    client.consume_queue(
                        queue_name=self._queue_move_responses,
                        routing_key=self._routing_key_move_response,
                        callback=self._handle_move_response,
                        auto_ack=False
                    )
                    # If we get here, consuming stopped normally
                    break
                except Exception as e:
                    retry_count += 1
                    logger.error(f"Error consuming move responses (attempt {retry_count}/{max_retries}): {e}",
                                 exc_info=True)

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
                        logger.error(f"Failed to consume move responses after {max_retries} attempts")
                        self._consuming = False
                        break
        finally:
            try:
                client.disconnect()
            except:
                pass

    def _consume_game_start(self) -> None:
        """Consume game start messages."""
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
                    client.consume_queue(
                        queue_name=self._queue_session_started,
                        routing_key=self._routing_key_session_started,
                        callback=self._handle_game_start,
                        auto_ack=False
                    )
                    # If we get here, consuming stopped normally
                    break
                except Exception as e:
                    retry_count += 1
                    logger.error(f"Error consuming game start (attempt {retry_count}/{max_retries}): {e}", exc_info=True)

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
                        logger.error(f"Failed to consume game start after {max_retries} attempts")
                        self._consuming = False
                        break
        finally:
            try:
                client.disconnect()
            except:
                pass

    def _consume_game_end(self) -> None:
        """Consume game end messages."""
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
                    client.consume_queue(
                        queue_name=self._queue_session_ended,
                        routing_key=self._routing_key_session_ended,
                        callback=self._handle_game_end,
                        auto_ack=False
                    )
                    # If we get here, consuming stopped normally
                    break
                except Exception as e:
                    retry_count += 1
                    logger.error(f"Error consuming game end (attempt {retry_count}/{max_retries}): {e}", exc_info=True)

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
                        logger.error(f"Failed to consume game end after {max_retries} attempts")
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

        thread1 = threading.Thread(target=self._consume_game_start, daemon=True)
        thread2 = threading.Thread(target=self._consume_move_responses, daemon=True)
        thread3 = threading.Thread(target=self._consume_game_end, daemon=True)

        self._threads = [thread1, thread2, thread3]

        for thread in self._threads:
            thread.start()

        logger.info("Game logger consumers started")

    def stop_consuming(self) -> None:
        """Stop consuming messages."""
        self._consuming = False

        for thread in self._threads:
            thread.join(timeout=5)

        logger.info("Game logger consumer stopped")
