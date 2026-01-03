"""
Game session service for stateful game operations.
"""
import uuid
import logging
from typing import Optional, List, Dict, Any, Callable
from datetime import datetime

from app.core.game.factory import GameFactory
from app.core.game.interfaces import GameInterface, GameStateInterface, GameMoveInterface
from app.modules.games.domain.models import GameSession, GameSessionStatus
from app.modules.games.repositories.session_repository import GameSessionRepository

logger = logging.getLogger(__name__)


class GameSessionService:
    """Service for managing game sessions."""
    
    def __init__(
        self,
        game_factory: GameFactory,
        session_repository: GameSessionRepository,
        event_publisher: Optional[Callable[[str, Dict[str, Any]], None]] = None
    ) -> None:
        self.game_factory = game_factory
        self.session_repository = session_repository
        self.event_publisher = event_publisher
        self.session_repository.initialize()
    
    def create_session(
        self,
        session_id: Optional[str],
        game_id: str,
        game_type: str,
        lobby_id: Optional[str],
        player_ids: List[str],
        starting_player_id: str,
        configuration: Optional[Dict[str, Any]] = None
    ) -> GameSession:
        """Create a new game session."""
        if not session_id:
            session_id = str(uuid.uuid4())
        
        try:
            # For external games like chess, create a minimal state without using game factory
            if game_type == "chess":
                # Chess is managed externally, so we create a minimal tracking state
                initial_state = {
                    "game_type": "chess",
                    "player_ids": player_ids,
                    "current_player_id": starting_player_id,
                    "status": "active",
                    "external_game_id": configuration.get("externalGameId") if configuration else None,
                    "configuration": configuration or {}
                }
            else:
                # For internal games (like Connect Four), use game factory
                game = self.game_factory.create_game(game_type)
                initial_state = game.create_initial_state(
                    player_ids=player_ids,
                    starting_player_id=starting_player_id,
                    configuration=configuration or {}
                )
                initial_state = initial_state.to_dict()
            
            session = GameSession(
                session_id=session_id,
                game_id=game_id,
                game_type=game_type,
                lobby_id=lobby_id,
                player_ids=player_ids,
                current_player_id=starting_player_id,
                status=GameSessionStatus.ACTIVE,
                game_state=initial_state,
                total_moves=0,
                started_at=datetime.utcnow()
            )
            
            self.session_repository.save(session)
            logger.info(f"Created session: session_id={session_id}, game_type={game_type}")
            return session
        except Exception as e:
            logger.error(f"Failed to create session: {e}", exc_info=True)
            raise
    
    def get_session(self, session_id: str) -> Optional[GameSession]:
        """Get session by ID."""
        return self.session_repository.find_by_id(session_id)
    
    def get_match_history(self, player_id: str, limit: int = 20) -> List[GameSession]:
        """Get match history for a player.
        
        Args:
            player_id: The player ID
            limit: Maximum number of matches to return (default: 20)
            
        Returns:
            List of game sessions, ordered by most recent first
        """
        # Get finished sessions (completed matches)
        from ..domain.models import GameSessionStatus
        finished_sessions = self.session_repository.find_by_player_id(
            player_id=player_id,
            limit=limit,
            status=GameSessionStatus.FINISHED
        )
        return finished_sessions
    
    def abandon_session(self, session_id: str, player_id: str, force: bool = False) -> GameSession:
        """Abandon/terminate a game session.
        
        Args:
            session_id: The session ID to abandon
            player_id: The player ID abandoning the session
            force: If True, allow abandoning even if session is not active (for force leave)
        """
        session = self.get_session(session_id)
        if not session:
            raise ValueError(f"Session not found: {session_id}")
        
        # Check if already abandoned/finished (unless forcing)
        if not force and not session.is_active():
            raise ValueError(f"Session is not active: {session_id}")
        
        if player_id not in session.player_ids:
            raise ValueError(f"Player {player_id} is not in this session")
        
        # Only update status if not already finished/abandoned
        if session.is_active():
            # Mark session as abandoned
            session.status = GameSessionStatus.ABANDONED
            session.ended_at = datetime.utcnow()
            # Set the other player as winner if game was in progress
            if len(session.player_ids) == 2:
                other_player_id = [pid for pid in session.player_ids if pid != player_id]
                if other_player_id:
                    session.winner_id = other_player_id[0]
            
            self.session_repository.save(session)
            logger.info(f"Abandoned session: session_id={session_id}, player_id={player_id}")
        else:
            # Session already finished/abandoned - just log it
            logger.info(f"Force abandon requested for already finished session: session_id={session_id}, player_id={player_id}, status={session.status}")
        
        
        # Publish game.session.ended event
        if self.event_publisher:
            try:
                event = {
                    "eventId": str(uuid.uuid4()),
                    "timestamp": session.ended_at.isoformat() + "Z",
                    "session_id": session.session_id,
                    "game_id": session.game_id,
                    "game_type": session.game_type,
                    "status": "abandoned",
                    "winner_id": session.winner_id,
                    "abandoned_by": player_id,
                    "final_game_state": session.game_state,
                    "total_moves": session.total_moves,
                    "type": "GAME_SESSION_ENDED"
                }
                self.event_publisher("game.session.ended", event)
            except Exception as e:
                logger.error(f"Failed to publish session ended event: {e}", exc_info=True)
        
        return session
    
    def apply_move(
        self,
        session_id: str,
        player_id: str,
        move_data: Dict[str, Any]
    ) -> GameSession:
        """Apply a move to a session."""
        session = self.get_session(session_id)
        if not session:
            raise ValueError(f"Session not found: {session_id}")
        
        if not session.is_active():
            raise ValueError(f"Session is not active: {session_id}")
        
        if session.current_player_id != player_id:
            raise ValueError(f"It's not player {player_id}'s turn")
        
        try:
            game = self.game_factory.create_game(session.game_type)
            state = self._deserialize_state(session.game_type, session.game_state, game)
            move = self._create_move_from_data(session.game_type, move_data)
            new_state = game.apply_move(state, move, player_id)
            
            session.game_state = new_state.to_dict()
            session.total_moves += 1
            session.current_player_id = game.get_current_player_id(new_state)
            
            status = game.get_game_status(new_state)
            game_finished = status != "ongoing"
            
            if game_finished:
                winner_id = game.get_winner_id(new_state)
                session.finish(winner_id)
            
            self.session_repository.save(session)
            logger.info(f"Applied move to session: session_id={session_id}, move_number={session.total_moves}, finished={game_finished}")
            
            # Publish game.move.applied event
            if self.event_publisher:
                try:
                    event = {
                        "eventId": str(uuid.uuid4()),
                        "timestamp": datetime.utcnow().isoformat() + "Z",
                        "session_id": session.session_id,
                        "game_id": session.game_id,
                        "game_type": session.game_type,
                        "player_id": player_id,
                        "move_data": move_data,  # Frontend expects move_data, not move
                        "game_state": session.game_state,
                        "current_player_id": session.current_player_id,  # Frontend expects this at top level
                        "status": "finished" if game_finished else "active",
                        "winner_id": session.winner_id,
                        "type": "GAME_MOVE_APPLIED"
                    }
                    self.event_publisher("game.move.applied", event)
                except Exception as e:
                    logger.error(f"Failed to publish move applied event: {e}", exc_info=True)
                    # Don't fail the move if event publishing fails
            
            # Publish game.session.ended event if game finished
            if game_finished and self.event_publisher:
                try:
                    event = {
                        "eventId": str(uuid.uuid4()),
                        "timestamp": session.ended_at.isoformat() + "Z" if session.ended_at else datetime.utcnow().isoformat() + "Z",
                        "session_id": session.session_id,
                        "game_id": session.game_id,
                        "game_type": session.game_type,
                        "status": "finished",
                        "winner_id": session.winner_id,
                        "final_game_state": session.game_state,
                        "total_moves": session.total_moves,
                        "type": "GAME_SESSION_ENDED"
                    }
                    self.event_publisher("game.session.ended", event)
                except Exception as e:
                    logger.error(f"Failed to publish session ended event: {e}", exc_info=True)
            
            return session
        except Exception as e:
            logger.error(f"Failed to apply move: {e}", exc_info=True)
            raise
    
    def _deserialize_state(
        self,
        game_type: str,
        state_dict: Dict[str, Any],
        game: GameInterface
    ) -> GameStateInterface:
        """Deserialize game state."""
        if game_type == "connect_four":
            from app.modules.games.games.connect_four.models import ConnectFourState
            return ConnectFourState.from_dict(state_dict)
        raise ValueError(f"Unknown game type: {game_type}")
    
    def _create_move_from_data(self, game_type: str, move_data: Dict[str, Any]) -> GameMoveInterface:
        """Create move from data."""
        if game_type == "connect_four":
            from app.modules.games.games.connect_four.models import ConnectFourMove
            column = move_data.get("column")
            if column is None:
                raise ValueError("Missing 'column' in move data for Connect Four")
            return ConnectFourMove(column=column)
        raise ValueError(f"Unknown game type: {game_type}")

