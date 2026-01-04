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

# Try to import GameSessionRepository to get player_ids if needed
try:
    from app.modules.games.repositories.session_repository import GameSessionRepository
    _has_session_repo = True
except ImportError:
    _has_session_repo = False
    GameSessionRepository = None


class GameLoggerEventConsumer:
    """Consumer for game logger events (game-agnostic)."""

    def __init__(self, rabbitmq_client: Any, game_logger_service: GameLoggerService) -> None:
        self.rabbitmq_client = rabbitmq_client
        self.game_logger_service = game_logger_service

        self._queue_session_started = "game_logger.session_started"
        self._queue_move_responses = "game_logger.move_responses"
        self._queue_move_applied = "game_logger.move_applied"
        self._queue_session_ended = "game_logger.session_ended"

        self._routing_key_session_started = "game.session.started"
        self._routing_key_move_response = "game.move.response"
        self._routing_key_move_applied = "game.move.applied"
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
        # Support both snake_case and camelCase
        player_ids = data.get('playerIds', []) or data.get('player_ids', [])
        starting_player_id = data.get('startingPlayerId') or data.get('starting_player_id')
        if not starting_player_id and player_ids:
            # Default to first player if starting_player_id not provided
            starting_player_id = player_ids[0]
        elif not starting_player_id:
            # Fallback to empty string if no players
            starting_player_id = ''
        
        # Support both snake_case and camelCase for lobby_id and session_id
        lobby_id = data.get('lobbyId') or data.get('lobby_id')
        session_id = data.get('sessionId') or data.get('session_id')
        
        return GameStartedEvent(
            event_id=data.get('eventId', '') or data.get('event_id', ''),
            game_id=data.get('gameId', '') or data.get('game_id', ''),
            game_type=data.get('gameType', '') or data.get('game_type', ''),
            lobby_id=lobby_id,
            player_ids=player_ids,
            starting_player_id=starting_player_id,
            game_configuration=data.get('gameConfiguration', {}) or data.get('game_configuration', {}),
            timestamp=self._parse_timestamp(data.get('timestamp')),
            session_id=session_id,
        )

    def _dict_to_move_response_event(self, data: Dict[str, Any]) -> GameMoveResponseEvent:
        """Convert dictionary to GameMoveResponseEvent."""
        # Extract valid field (default to True if not provided, as moves are already applied)
        valid = data.get('valid')
        if valid is None:
            # If not explicitly set, assume valid=True for move.response events
            # (moves that reach this point are already validated and applied)
            valid = True
        
        return GameMoveResponseEvent(
            event_id=data.get('eventId', ''),
            game_id=data.get('gameId', ''),
            game_type=data.get('gameType', ''),
            player_id=data.get('playerId', ''),
            move=data.get('move', {}),
            new_game_state=data.get('newGameState', {}),
            valid=valid,
            game_status=data.get('gameStatus', 'ongoing'),
            timestamp=self._parse_timestamp(data.get('timestamp')),
        )

    def _dict_to_game_ended_event(self, data: Dict[str, Any]) -> GameEndedEvent:
        """Convert dictionary to GameEndedEvent."""
        # Support both snake_case and camelCase field names
        winner_id = data.get('winner_id') or data.get('winnerId')
        
        return GameEndedEvent(
            event_id=data.get('eventId', ''),
            game_id=data.get('gameId', ''),
            game_type=data.get('gameType', ''),
            winner_id=winner_id,
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
            # Use session_id from event if available, otherwise fallback to game_id
            # For Connect Four: session_id is different from game_id (session_id comes from game.session.started)
            # For Chess: session_id = game_id (they are the same)
            session_id = event.session_id or event.game_id
            
            if not session_id:
                logger.error(f"No session_id or game_id in event: {event}")
                return
            
            # For chess games, always create session even if player_ids is empty
            # (player lookup might fail, but we still need to log moves)
            if event.game_type == "chess" or len(event.player_ids) >= 2:
                self.game_logger_service.create_session(
                    session_id=session_id,
                    game_type=event.game_type,
                    p1_type="human",
                    p2_type="human",
                    tag=f"lobby_{event.lobby_id}" if event.lobby_id else None,
                )
                logger.info(f"Created game session: session_id={session_id}, game_type={event.game_type}, lobby_id={event.lobby_id}, player_ids_count={len(event.player_ids)}")
            else:
                logger.warning(f"Skipping session creation - insufficient player_ids: session_id={session_id}, game_type={event.game_type}, player_ids_count={len(event.player_ids)}")
        except Exception as e:
            logger.error(f"Failed to create game session: {e}", exc_info=True)

    def _handle_move_response(self, message: Dict[str, Any]) -> None:
        """Handle move response message."""
        try:
            event = self._dict_to_move_response_event(message)
            self._handle_move_response_event(event)
        except Exception as e:
            logger.error(f"Failed to handle move response: {e}", exc_info=True)

    def _handle_move_applied(self, message: Dict[str, Any]) -> None:
        """Handle move applied message (for chess and other games where moves are already applied)."""
        try:
            # For move.applied events, the structure is similar to move.response
            # Extract session_id and game_id (support both snake_case and camelCase)
            # Connect Four uses session_id, chess uses game_id as session_id
            session_id = message.get("session_id") or message.get("sessionId")
            game_id = message.get("game_id") or message.get("gameId")
            
            # For chess games, game_id is the session_id
            if not session_id:
                session_id = game_id
            
            # Fallback: use session_id as game_id if game_id not provided
            if not game_id:
                game_id = session_id
            
            if not game_id:
                logger.warning(f"Received game.move.applied without game_id or session_id: {message}")
                return
            
            # Extract move data (Connect Four uses "move_data", chess uses "move")
            move_data = message.get("move_data") or message.get("move", {})
            if not move_data:
                logger.warning(f"No move data in game.move.applied: game_id={game_id}, message_keys={list(message.keys())}")
                return
            
            # Extract player_id (support both snake_case and camelCase)
            player_id = message.get("player_id") or message.get("playerId")
            if not player_id:
                # Try to get from move data
                player_id = move_data.get("playerId") or move_data.get("player_id") or "unknown"
            
            # Extract game type
            game_type = message.get("game_type") or message.get("gameType") or "chess"
            
            # Get new game state (from move data or message)
            # Connect Four uses "game_state", chess uses "newGameState" or "fenAfterMove"
            new_game_state = message.get("game_state") or message.get("newGameState") or move_data.get("fenAfterMove") or move_data
            game_status = message.get("status") or message.get("gameStatus") or message.get("game_status") or "ongoing"
            
            # For move.applied events, moves are already validated and applied, so valid=True
            valid = message.get("valid")
            if valid is None:
                valid = True  # Moves that reach this point are already applied, so they're valid
            
            # Create a move response event-like structure
            event = GameMoveResponseEvent(
                event_id=message.get("eventId") or message.get("event_id") or "",
                game_id=game_id,
                game_type=game_type,
                player_id=player_id,
                move=move_data,
                new_game_state=new_game_state if isinstance(new_game_state, dict) else {"state": new_game_state},
                valid=valid,
                game_status=game_status,
                timestamp=self._parse_timestamp(message.get("timestamp")),
            )
            
            # Pass session_id explicitly (Connect Four uses session_id, chess uses game_id as session_id)
            self._handle_move_response_event(event, session_id=session_id)
        except Exception as e:
            logger.error(f"Failed to handle move applied: {e}", exc_info=True)

    def _handle_move_response_event(self, event: GameMoveResponseEvent, session_id: Optional[str] = None) -> None:
        """Handle move response event."""
        try:
            game_id = event.game_id
            game_type = event.game_type
            state_after = event.new_game_state

            move_data = event.move
            if not move_data:
                logger.warning(f"No move data: game_id={game_id}")
                return

            # Use provided session_id, or fallback to game_id (for chess games, they are the same)
            if not session_id:
                session_id = game_id
            
            # Get move number from state or move data
            move_number = state_after.get('move_number', 0) if isinstance(state_after, dict) else 0
            if move_number == 0:
                move_number = move_data.get('moveNumber', 0) if isinstance(move_data, dict) else 0
            if move_number == 0:
                # Fallback: count existing moves + 1
                existing_moves = self.game_logger_service.get_session_moves(session_id)
                move_number = len(existing_moves) + 1

            # For chess games, use simplified logging (no ML training data required)
            if game_type == "chess":
                self.game_logger_service.log_chess_move(
                    session_id=session_id,
                    move_number=move_number,
                    player_id=event.player_id,
                    move_data=move_data,
                    game_state=state_after,
                    game_status=event.game_status,
                )
            elif game_type == "connect_four":
                # For Connect Four, use event-based logging (extracts available data from events)
                self.game_logger_service.log_connect_four_move(
                    session_id=session_id,
                    move_number=move_number,
                    player_id=event.player_id,
                    move_data=move_data,
                    game_state=state_after,
                    game_status=event.game_status,
                )
            else:
                # For other games, use simplified logging as fallback
                logger.info(f"Using simplified logging for game_type={game_type} (ML training data not available in events)")
                self.game_logger_service.log_chess_move(
                    session_id=session_id,
                    move_number=move_number,
                    player_id=event.player_id,
                    move_data=move_data,
                    game_state=state_after,
                    game_status=event.game_status,
                )

            logger.info(f"Logged move: session_id={session_id}, game_type={game_type}, move_number={move_number}")

        except Exception as e:
            logger.error(f"Failed to log move: {e}", exc_info=True)

    def _handle_game_end(self, message: Dict[str, Any]) -> None:
        """Handle game end message."""
        try:
            event = self._dict_to_game_ended_event(message)

            # Extract session_id from message (Connect Four uses session_id, chess uses game_id as session_id)
            # Support both snake_case and camelCase
            session_id = message.get("session_id") or message.get("sessionId")
            if not session_id:
                # For chess games, game_id is the session_id
                session_id = event.game_id
            
            if not session_id:
                logger.error(f"Cannot determine session_id from game ended event: {message}")
                return

            # Extract total moves from final_game_state
            final_state = event.final_game_state
            total_moves = final_state.get('move_number', 0) if isinstance(final_state, dict) else 0
            # Also try total_moves from the message directly
            if total_moves == 0:
                total_moves = message.get('total_moves', 0)

            # Determine winner: 'p1', 'p2', or 'draw'
            winner = 'draw'
            if event.winner_id:
                # Try to get player_ids from message first
                player_ids = message.get('player_ids') or message.get('playerIds', [])
                
                # If not in message, try to get from game session repository
                if not player_ids and _has_session_repo:
                    try:
                        from app.shared.container import container
                        if container.has(GameSessionRepository):
                            session_repo = container.get(GameSessionRepository)
                            game_session = session_repo.find_by_id(session_id)
                            if game_session and hasattr(game_session, 'player_ids'):
                                player_ids = game_session.player_ids
                                logger.debug(f"Retrieved player_ids from game session: {player_ids}")
                    except Exception as e:
                        logger.warn(f"Failed to get player_ids from game session: {e}")
                
                if len(player_ids) >= 2:
                    # Map winner_id to p1 or p2 based on player order
                    if event.winner_id == player_ids[0]:
                        winner = 'p1'
                    elif event.winner_id == player_ids[1]:
                        winner = 'p2'
                    else:
                        # Winner not in player list, default to draw
                        logger.warn(f"Winner ID {event.winner_id} not found in player_ids {player_ids}, defaulting to draw")
                        winner = 'draw'
                else:
                    # No player_ids available, default to draw
                    logger.warn(f"Cannot determine p1 vs p2 without player_ids for winner_id={event.winner_id}, defaulting to draw")
                    winner = 'draw'

            # Call finish_session with correct parameters
            self.game_logger_service.finish_session(
                session_id=session_id,
                winner=winner,
                num_moves=total_moves,
                end_time=event.timestamp,
            )

            logger.info(f"Finished game session: session_id={session_id}, winner={winner}, moves={total_moves}")

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

    def _consume_move_applied(self) -> None:
        """Consume move applied messages (for chess and other games)."""
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
                        queue_name=self._queue_move_applied,
                        routing_key=self._routing_key_move_applied,
                        callback=self._handle_move_applied,
                        auto_ack=False
                    )
                    # If we get here, consuming stopped normally
                    break
                except Exception as e:
                    retry_count += 1
                    logger.error(f"Error consuming move applied (attempt {retry_count}/{max_retries}): {e}",
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
                        logger.error(f"Failed to consume move applied after {max_retries} attempts")
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
        thread3 = threading.Thread(target=self._consume_move_applied, daemon=True)
        thread4 = threading.Thread(target=self._consume_game_end, daemon=True)

        self._threads = [thread1, thread2, thread3, thread4]

        for thread in self._threads:
            thread.start()

        logger.info("Game logger consumers started")

    def stop_consuming(self) -> None:
        """Stop consuming messages."""
        self._consuming = False

        for thread in self._threads:
            thread.join(timeout=5)

        logger.info("Game logger consumer stopped")
