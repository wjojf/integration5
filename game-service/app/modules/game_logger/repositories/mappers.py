"""
Mappers between domain data and ORM models for ML-focused game logging.
"""
from typing import Optional, Dict, Any, List
from datetime import datetime

from .models import SessionLogModel, GameMoveModel


class SessionLogMapper:
    """Mapper for SessionLog (game session metadata)."""

    @staticmethod
    def to_orm(
        session_id: str,
        game_type: str,
        p1_type: str,
        p2_type: str,
        p1_agent: Optional[str] = None,
        p2_agent: Optional[str] = None,
        p1_agent_level: Optional[str] = None,
        p2_agent_level: Optional[str] = None,
        winner: Optional[str] = None,
        num_moves: int = 0,
        duration_seconds: Optional[float] = None,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        tag: Optional[str] = None,
    ) -> SessionLogModel:
        """Create ORM model from parameters."""
        return SessionLogModel(
            session_id=session_id,
            game_type=game_type,
            p1_type=p1_type,
            p2_type=p2_type,
            p1_agent=p1_agent,
            p2_agent=p2_agent,
            p1_agent_level=p1_agent_level,
            p2_agent_level=p2_agent_level,
            winner=winner,
            num_moves=num_moves,
            duration_seconds=duration_seconds,
            start_time=start_time or datetime.utcnow(),
            end_time=end_time,
            tag=tag,
        )

    @staticmethod
    def to_dict(model: SessionLogModel) -> Dict[str, Any]:
        """Convert ORM model to dictionary."""
        return {
            "session_id": model.session_id,
            "game_type": model.game_type,
            "p1_type": model.p1_type,
            "p2_type": model.p2_type,
            "p1_agent": model.p1_agent,
            "p2_agent": model.p2_agent,
            "p1_agent_level": model.p1_agent_level,
            "p2_agent_level": model.p2_agent_level,
            "winner": model.winner,
            "num_moves": model.num_moves,
            "duration_seconds": model.duration_seconds,
            "start_time": model.start_time.isoformat() if model.start_time else None,
            "end_time": model.end_time.isoformat() if model.end_time else None,
            "tag": model.tag,
        }


class GameMoveMapper:
    """Mapper for GameMove (ML training data with expert labels)."""

    @staticmethod
    def to_orm(
        session_id: str,
        move_number: int,
        current_player: str,
        current_player_id: int,
        board_state: Dict[str, Any],
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
    ) -> GameMoveModel:
        """
        Create ORM model from parameters with validation.

        Raises:
            AssertionError: If validation fails
        """
        # Validation
        assert len(board_flat) == 42, f"board_flat must be 42 elements, got {len(board_flat)}"
        assert len(legal_moves_mask) == 7, f"legal_moves_mask must be 7 elements, got {len(legal_moves_mask)}"
        assert len(expert_policy) == 7, f"expert_policy must be 7 elements, got {len(expert_policy)}"
        assert current_player_id in [1, 2], f"current_player_id must be 1 or 2, got {current_player_id}"
        assert 0 <= played_move <= 6, f"played_move must be 0-6, got {played_move}"
        assert 0 <= expert_value <= 1, f"expert_value must be 0-1, got {expert_value}"
        assert all(val in [0, 1, 2] for val in board_flat), "board_flat must contain only 0, 1, 2"
        assert all(val in [0, 1] for val in legal_moves_mask), "legal_moves_mask must contain only 0, 1"

        return GameMoveModel(
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

    @staticmethod
    def to_dict(model: GameMoveModel) -> Dict[str, Any]:
        """Convert ORM model to dictionary."""
        return {
            "id": model.id,
            "session_id": model.session_id,
            "move_number": model.move_number,
            "current_player": model.current_player,
            "current_player_id": model.current_player_id,
            "board_state": model.board_state,
            "board_flat": model.board_flat,
            "legal_moves_mask": model.legal_moves_mask,
            "played_move": model.played_move,
            "expert_policy": model.expert_policy,
            "expert_best_move": model.expert_best_move,
            "expert_value": model.expert_value,
            "final_result": model.final_result,
            "final_result_numeric": model.final_result_numeric,
            "mcts_iterations": model.mcts_iterations,
            "game_status": model.game_status,
            "timestamp": model.timestamp.isoformat() if model.timestamp else None,
        }








