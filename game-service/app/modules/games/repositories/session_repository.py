"""
Game session repository for persistence.
"""
from typing import Optional, List
from contextlib import contextmanager
import logging

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.config import settings
from ..domain.models import GameSession, GameSessionStatus
from .models import Base, GameSessionModel
from .mappers import GameSessionMapper

logger = logging.getLogger(__name__)


class GameSessionRepository:
    """Repository for game session persistence."""

    def __init__(self) -> None:
        self.engine = create_engine(
            settings.DATABASE_URL,
            pool_size=settings.DB_POOL_SIZE,
            max_overflow=settings.DB_MAX_OVERFLOW,
            echo=settings.DEBUG,
        )
        self.SessionLocal = sessionmaker(bind=self.engine)
        self._initialized: bool = False

    def initialize(self) -> None:
        """Initialize database tables."""
        if not self._initialized:
            Base.metadata.create_all(bind=self.engine)
            self._initialized = True

    @contextmanager
    def get_session(self):
        """Get database session context manager."""
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def save(self, session: GameSession) -> GameSession:
        """Save or update a game session."""
        self.initialize()

        with self.get_session() as db_session:
            orm_model = db_session.query(GameSessionModel).filter(
                GameSessionModel.session_id == session.session_id
            ).first()

            if orm_model:
                GameSessionMapper.update_orm_from_domain(orm_model, session)
            else:
                orm_model = GameSessionMapper.to_orm(session)
                db_session.add(orm_model)

            db_session.flush()
            return GameSessionMapper.to_domain(orm_model)

    def find_by_id(self, session_id: str) -> Optional[GameSession]:
        """Find session by ID."""
        self.initialize()

        with self.get_session() as db_session:
            orm_model = db_session.query(GameSessionModel).filter(
                GameSessionModel.session_id == session_id
            ).first()

            if orm_model:
                return GameSessionMapper.to_domain(orm_model)
            return None

    def find_by_game_id(self, game_id: str) -> List[GameSession]:
        """Find all sessions for a game."""
        self.initialize()

        with self.get_session() as db_session:
            orm_models = db_session.query(GameSessionModel).filter(
                GameSessionModel.game_id == game_id
            ).all()

            return [GameSessionMapper.to_domain(orm) for orm in orm_models]

    def find_active_by_game_id(self, game_id: str) -> List[GameSession]:
        """Find active sessions for a game."""
        self.initialize()

        with self.get_session() as db_session:
            orm_models = db_session.query(GameSessionModel).filter(
                GameSessionModel.game_id == game_id,
                GameSessionModel.status == GameSessionStatus.ACTIVE
            ).all()

            return [GameSessionMapper.to_domain(orm) for orm in orm_models]

    def find_by_player_id(self, player_id: str, limit: int = 50, status: Optional[GameSessionStatus] = None) -> List[GameSession]:
        """Find all sessions for a player.
        
        Args:
            player_id: The player ID to search for
            limit: Maximum number of sessions to return
            status: Optional status filter (e.g., GameSessionStatus.FINISHED)
        """
        self.initialize()

        with self.get_session() as db_session:
            # Get all sessions and filter in Python (since player_ids is JSON array)
            # This is simpler and more compatible than JSONB operators
            query = db_session.query(GameSessionModel)
            
            if status:
                query = query.filter(GameSessionModel.status == status)
            
            # Order by most recent first
            query = query.order_by(GameSessionModel.started_at.desc())
            
            all_orm_models = query.all()
            
            # Filter by player_id in player_ids JSON array
            matching_sessions = []
            for orm in all_orm_models:
                # player_ids is stored as JSON, which SQLAlchemy deserializes to Python list
                if isinstance(orm.player_ids, list) and player_id in orm.player_ids:
                    matching_sessions.append(orm)
                    if len(matching_sessions) >= limit:
                        break
            
            return [GameSessionMapper.to_domain(orm) for orm in matching_sessions]















