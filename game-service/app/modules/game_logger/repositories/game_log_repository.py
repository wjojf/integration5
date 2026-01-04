"""
Repository for ML-focused game logging.
Handles database operations for session logs and game moves.
"""
from typing import Optional, List, Dict
from contextlib import contextmanager
from datetime import datetime
from sqlalchemy import create_engine, desc
from sqlalchemy.orm import sessionmaker, Session
from app.config import settings
from .models import Base, SessionLogModel, GameMoveModel
from .mappers import SessionLogMapper, GameMoveMapper
import logging

logger = logging.getLogger(__name__)


class GameLogRepository:
    """Repository for ML-focused game logging."""

    def __init__(self) -> None:
        """Initialize database connection with connection pooling."""
        db_url = (
            f"postgresql://{settings.DB_USER}:{settings.DB_PASSWORD}"
            f"@{settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME}"
        )
        self.engine = create_engine(
            db_url,
            pool_size=10,
            max_overflow=20,
            pool_pre_ping=True,
            echo=False,  # Set to True for SQL debugging
        )
        self.SessionLocal = sessionmaker(bind=self.engine, expire_on_commit=False)
        self._initialized = False
        logger.info("GameLogRepository initialized with PostgreSQL connection")

    def initialize(self) -> None:
        """Create tables if they don't exist."""
        if not self._initialized:
            Base.metadata.create_all(self.engine)
            self._initialized = True
            logger.info("Database tables initialized (session_logs, game_moves)")

    @contextmanager
    def get_session(self) -> Session:
        """
        Context manager for database sessions.
        Handles commit/rollback automatically.
        """
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except Exception as e:
            session.rollback()
            logger.error(f"Session rollback due to error: {e}")
            raise
        finally:
            session.close()

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
        """
        Create a new game session.

        Args:
            session_id: Unique session identifier
            game_type: Type of game (e.g., 'connect_four')
            p1_type: Player 1 type ('human' or 'ai')
            p2_type: Player 2 type ('human' or 'ai')
            p1_agent: Player 1 agent configuration
            p2_agent: Player 2 agent configuration
            p1_agent_level: Player 1 difficulty level
            p2_agent_level: Player 2 difficulty level
            tag: Experiment tag for dataset organization

        Returns:
            Dictionary representation of created session, or None on failure
        """
        self.initialize()

        with self.get_session() as db_session:
            try:
                # Check if session already exists (idempotent creation)
                existing = db_session.query(SessionLogModel).filter_by(session_id=session_id).first()
                if existing:
                    logger.info(f"Session already exists: {session_id}, returning existing session")
                    return SessionLogMapper.to_dict(existing)
                
                session_model = SessionLogMapper.to_orm(
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
                db_session.add(session_model)
                db_session.flush()
                db_session.refresh(session_model)

                result = SessionLogMapper.to_dict(session_model)
                logger.info(f"Session created: {session_id}")
                return result
            except Exception as e:
                # If duplicate key error, try to get existing session
                from sqlalchemy.exc import IntegrityError
                if isinstance(e, IntegrityError) and "duplicate key" in str(e).lower():
                    logger.warning(f"Session {session_id} already exists (duplicate key), retrieving existing session")
                    try:
                        existing = db_session.query(SessionLogModel).filter_by(session_id=session_id).first()
                        if existing:
                            return SessionLogMapper.to_dict(existing)
                    except Exception as e2:
                        logger.error(f"Failed to retrieve existing session {session_id}: {e2}")
                logger.error(f"Failed to create session {session_id}: {e}")
                raise

    def finish_session(
        self,
        session_id: str,
        winner: str,
        num_moves: int,
        duration_seconds: Optional[float] = None,
        end_time: Optional[datetime] = None,
    ) -> Optional[Dict]:
        """
        Mark a session as finished with final results.

        Args:
            session_id: Session ID to finish
            winner: Winner ('p1', 'p2', or 'draw')
            num_moves: Total number of moves in the game
            duration_seconds: Match duration in seconds
            end_time: Match end timestamp

        Returns:
            Updated session dictionary, or None on failure
        """
        self.initialize()

        with self.get_session() as db_session:
            try:
                session_model = db_session.query(SessionLogModel).filter_by(
                    session_id=session_id
                ).first()

                if not session_model:
                    raise ValueError(f"Session {session_id} not found")

                session_model.winner = winner
                session_model.num_moves = num_moves
                session_model.duration_seconds = duration_seconds
                session_model.end_time = end_time or datetime.utcnow()

                db_session.flush()
                db_session.refresh(session_model)

                result = SessionLogMapper.to_dict(session_model)
                logger.info(f"Session finished: {session_id}, winner={winner}, moves={num_moves}")
                return result
            except Exception as e:
                logger.error(f"Failed to finish session {session_id}: {e}")
                raise

    def get_session_info(self, session_id: str) -> Optional[Dict]:
        """
        Get session by ID.

        Args:
            session_id: Session ID to retrieve

        Returns:
            Session dictionary or None if not found
        """
        self.initialize()

        with self.get_session() as db_session:
            session_model = db_session.query(SessionLogModel).filter_by(
                session_id=session_id
            ).first()

            if session_model:
                return SessionLogMapper.to_dict(session_model)
            return None

    def get_sessions_by_tag(self, tag: str, limit: int = 100) -> List[Dict]:
        """
        Get sessions by experiment tag.

        Args:
            tag: Experiment tag to filter by
            limit: Maximum number of sessions to return

        Returns:
            List of session dictionaries
        """
        self.initialize()

        with self.get_session() as db_session:
            sessions = db_session.query(SessionLogModel).filter_by(
                tag=tag
            ).order_by(desc(SessionLogModel.start_time)).limit(limit).all()

            return [SessionLogMapper.to_dict(s) for s in sessions]

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
        """
        Log a single move with ML training data.

        Args:
            session_id: Session this move belongs to
            move_number: Move sequence number (1-indexed)
            current_player: Current player ('p1' or 'p2')
            current_player_id: Normalized player ID (1 or -1)
            board_state: Raw board state as dict
            board_flat: Flattened board [42 elements]
            legal_moves_mask: Binary mask [7 elements]
            played_move: Column played (0-6)
            expert_policy: MCTS probability distribution [7 elements]
            expert_best_move: Best move from expert
            expert_value: Win probability [0-1]
            final_result: Game outcome ('win', 'loss', 'draw', 'ongoing')
            final_result_numeric: Numeric result (1.0/0.0/0.5)
            mcts_iterations: Number of MCTS iterations
            game_status: Game status ('ongoing' or 'finished')

        Returns:
            Dictionary representation of logged move, or None on failure
        """
        self.initialize()

        with self.get_session() as db_session:
            try:
                move_model = GameMoveMapper.to_orm(
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
                db_session.add(move_model)
                db_session.flush()
                db_session.refresh(move_model)

                result = GameMoveMapper.to_dict(move_model)
                logger.debug(f"Move logged: session={session_id}, move={move_number}")
                return result
            except Exception as e:
                logger.error(f"Failed to log move {move_number} for session {session_id}: {e}")
                raise

    def get_session_moves(self, session_id: str) -> List[Dict]:
        """
        Get all moves for a session, ordered by move_number.

        Args:
            session_id: Session ID to retrieve moves for

        Returns:
            List of move dictionaries ordered by move_number
        """
        self.initialize()

        with self.get_session() as db_session:
            moves = db_session.query(GameMoveModel).filter_by(
                session_id=session_id
            ).order_by(GameMoveModel.move_number).all()

            return [GameMoveMapper.to_dict(move) for move in moves]

    def get_move(self, move_id: int) -> Optional[Dict]:
        """
        Get a specific move by ID.

        Args:
            move_id: Move ID to retrieve

        Returns:
            Move dictionary or None if not found
        """
        self.initialize()

        with self.get_session() as db_session:
            move = db_session.query(GameMoveModel).filter_by(id=move_id).first()

            if move:
                return GameMoveMapper.to_dict(move)
            return None

    def get_recent_moves(self, limit: int = 100) -> List[Dict]:
        """
        Get recent moves across all sessions.

        Args:
            limit: Maximum number of moves to return

        Returns:
            List of move dictionaries ordered by timestamp (newest first)
        """
        self.initialize()

        with self.get_session() as db_session:
            moves = db_session.query(GameMoveModel).order_by(
                desc(GameMoveModel.timestamp)
            ).limit(limit).all()

            return [GameMoveMapper.to_dict(move) for move in moves]

    def count_moves_by_session(self, session_id: str) -> int:
        """
        Count total moves for a session.

        Args:
            session_id: Session ID to count moves for

        Returns:
            Number of moves in the session
        """
        self.initialize()

        with self.get_session() as db_session:
            return db_session.query(GameMoveModel).filter_by(
                session_id=session_id
            ).count()

    def delete_session(self, session_id: str) -> bool:
        """
        Delete a session and all its moves (CASCADE).

        Args:
            session_id: Session ID to delete

        Returns:
            True if deleted, False if not found
        """
        self.initialize()

        with self.get_session() as db_session:
            try:
                session_model = db_session.query(SessionLogModel).filter_by(
                    session_id=session_id
                ).first()

                if not session_model:
                    logger.warning(f"Session {session_id} not found for deletion")
                    return False

                db_session.delete(session_model)
                db_session.flush()
                logger.info(f"Session {session_id} deleted (CASCADE)")
                return True
            except Exception as e:
                logger.error(f"Failed to delete session {session_id}: {e}")
                raise

    # ==================== STATISTICS ====================

    def get_stats(self) -> Dict:
        """
        Get overall database statistics.

        Returns:
            Dictionary with database statistics
        """
        self.initialize()

        with self.get_session() as db_session:
            total_sessions = db_session.query(SessionLogModel).count()
            total_moves = db_session.query(GameMoveModel).count()
            finished_sessions = db_session.query(SessionLogModel).filter(
                SessionLogModel.end_time.isnot(None)
            ).count()

            return {
                "total_sessions": total_sessions,
                "total_moves": total_moves,
                "finished_sessions": finished_sessions,
                "ongoing_sessions": total_sessions - finished_sessions,
                "avg_moves_per_session": total_moves / total_sessions if total_sessions > 0 else 0,
            }
