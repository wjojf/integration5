"""
Game logger service - ML-focused.
Business logic layer for ML training data logging.
"""
from typing import Optional, Dict, List
from datetime import datetime
import logging

from app.config import settings
from .repositories.game_log_repository import GameLogRepository

logger = logging.getLogger(__name__)


class GameLoggerService:
    """Service for ML-focused game logging."""

    def __init__(self, repository: Optional[GameLogRepository] = None) -> None:
        """Initialize service with repository."""
        self.repository = repository or GameLogRepository()
        if settings.LOGGER_ENABLED:
            self.repository.initialize()

    # ==================== SESSION OPERATIONS ====================

    def create_session(
        self,
        session_id: str,
        game_type: str,
        p1_type: str,
        p2_type: str,
        p1_agent: Optional[str] = None,
        p2_agent: Optional[str] = None,
        p1_agent_level: Optional[str] = None,
        p2_agent_level: Optional[str] = None,
        tag: Optional[str] = None,
    ) -> Optional[Dict]:
        """Create a new game session."""
        if not settings.LOGGER_ENABLED:
            logger.info("Logger is disabled")
            return None

        try:
            result = self.repository.create_session(
                session_id=session_id,
                game_type=game_type,
                p1_type=p1_type,
                p2_type=p2_type,
                p1_agent=p1_agent,
                p2_agent=p2_agent,
                p1_agent_level=p1_agent_level,
                p2_agent_level=p2_agent_level,
                tag=tag,
            )
            logger.info(f"Session created: {session_id}")
            return result
        except Exception as e:
            logger.error(f"Failed to create session: {e}", exc_info=True)
            return None

    def finish_session(
        self,
        session_id: str,
        winner: str,
        num_moves: int,
        duration_seconds: Optional[float] = None,
        end_time: Optional[datetime] = None,
    ) -> Optional[Dict]:
        """Mark session as finished."""
        if not settings.LOGGER_ENABLED:
            return None

        try:
            result = self.repository.finish_session(
                session_id=session_id,
                winner=winner,
                num_moves=num_moves,
                duration_seconds=duration_seconds,
                end_time=end_time,
            )
            logger.info(f"Session finished: {session_id}, winner={winner}")
            return result
        except Exception as e:
            logger.error(f"Failed to finish session: {e}", exc_info=True)
            return None

    def get_session(self, session_id: str) -> Optional[Dict]:
        """Get session by ID."""
        try:
            return self.repository.get_session_info(session_id)
        except Exception as e:
            logger.error(f"Failed to get session: {e}", exc_info=True)
            return None

    def get_sessions_by_tag(self, tag: str, limit: int = 100) -> List[Dict]:
        """Get sessions by experiment tag."""
        try:
            return self.repository.get_sessions_by_tag(tag, limit)
        except Exception as e:
            logger.error(f"Failed to get sessions by tag: {e}", exc_info=True)
            return []

    # ==================== MOVE OPERATIONS ====================

    def log_move(
        self,
        session_id: str,
        move_number: int,
        current_player: str,
        current_player_id: int,
        board_state: dict,
        board_flat: List[int],
        legal_moves_mask: List[int],
        played_move: int,
        expert_policy: List[float],
        expert_best_move: int,
        expert_value: float,
        final_result: Optional[str] = None,
        final_result_numeric: Optional[float] = None,
        mcts_iterations: Optional[int] = None,
        game_status: Optional[str] = None,
    ) -> Optional[Dict]:
        """Log a move with ML training data."""
        if not settings.LOGGER_ENABLED:
            return None

        try:
            result = self.repository.log_move(
                session_id=session_id,
                move_number=move_number,
                current_player=current_player,
                current_player_id=current_player_id,
                board_state=board_state,
                board_flat=board_flat,
                legal_moves_mask=legal_moves_mask,
                played_move=played_move,
                expert_policy=expert_policy,
                expert_best_move=expert_best_move,
                expert_value=expert_value,
                final_result=final_result,
                final_result_numeric=final_result_numeric,
                mcts_iterations=mcts_iterations,
                game_status=game_status,
            )
            logger.debug(f"Move logged: session={session_id}, move={move_number}")
            return result
        except Exception as e:
            logger.error(f"Failed to log move: {e}", exc_info=True)
            return None

    def get_session_moves(self, session_id: str) -> List[Dict]:
        """Get all moves for a session."""
        try:
            return self.repository.get_session_moves(session_id)
        except Exception as e:
            logger.error(f"Failed to get session moves: {e}", exc_info=True)
            return []

    def get_move(self, move_id: int) -> Optional[Dict]:
        """Get a specific move by ID."""
        try:
            return self.repository.get_move(move_id)
        except Exception as e:
            logger.error(f"Failed to get move: {e}", exc_info=True)
            return None

    def get_recent_moves(self, limit: int = 100) -> List[Dict]:
        """Get recent moves across all sessions."""
        try:
            return self.repository.get_recent_moves(limit)
        except Exception as e:
            logger.error(f"Failed to get recent moves: {e}", exc_info=True)
            return []

    def log_chess_move(
        self,
        session_id: str,
        move_number: int,
        player_id: str,
        move_data: Dict,
        game_state: Dict,
        game_status: str = "ongoing",
    ) -> Optional[Dict]:
        """
        Log a chess move with simplified data structure.
        For chess games, we don't need all the ML training data.
        """
        if not settings.LOGGER_ENABLED:
            return None

        try:
            # For chess, we store move data in board_state as JSON
            # Create a simplified structure that works with the existing repository
            board_state = {
                "move": move_data,
                "game_state": game_state,
                "game_status": game_status,
            }
            
            # Determine current_player based on move data or move number
            current_player = "p1" if move_number % 2 == 1 else "p2"
            current_player_id = 1 if move_number % 2 == 1 else 2
            
            # For chess, we don't have ML training data, so use defaults
            # The repository expects these fields, but they're not used for chess
            result = self.repository.log_move(
                session_id=session_id,
                move_number=move_number,
                current_player=current_player,
                current_player_id=current_player_id,
                board_state=board_state,
                board_flat=[0] * 42,  # Placeholder for Connect Four format
                legal_moves_mask=[1] * 7,  # Placeholder
                played_move=0,  # Placeholder
                expert_policy=[0.0] * 7,  # Placeholder
                expert_best_move=0,  # Placeholder
                expert_value=0.5,  # Placeholder
                final_result=game_status,
                game_status=game_status,
            )
            logger.debug(f"Chess move logged: session={session_id}, move={move_number}")
            return result
        except Exception as e:
            logger.error(f"Failed to log chess move: {e}", exc_info=True)
            return None

    # ==================== STATISTICS ====================

    def get_stats(self) -> Dict:
        """Get overall database statistics."""
        try:
            return self.repository.get_stats()
        except Exception as e:
            logger.error(f"Failed to get stats: {e}", exc_info=True)
            return {
                "total_sessions": 0,
                "total_moves": 0,
                "finished_sessions": 0,
                "ongoing_sessions": 0,
                "avg_moves_per_session": 0,
            }
