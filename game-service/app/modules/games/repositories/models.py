"""
SQLAlchemy ORM models for game sessions.
"""
from datetime import datetime

from sqlalchemy import Column, String, Integer, DateTime, JSON, Enum as SQLEnum
from sqlalchemy.orm import declarative_base

from ..domain.models import GameSessionStatus

Base = declarative_base()


class GameSessionModel(Base):
    """SQLAlchemy model for game sessions."""
    __tablename__ = "game_sessions"
    
    session_id = Column(String(64), primary_key=True)
    game_id = Column(String(64), nullable=False, index=True)
    game_type = Column(String(32), nullable=False, index=True)
    lobby_id = Column(String(64), nullable=True, index=True)
    player_ids = Column(JSON, nullable=False)
    current_player_id = Column(String(64), nullable=False)
    status = Column(SQLEnum(GameSessionStatus), nullable=False, index=True)
    game_state = Column(JSON, nullable=False)
    started_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    ended_at = Column(DateTime, nullable=True)
    winner_id = Column(String(64), nullable=True)
    total_moves = Column(Integer, nullable=False, default=0)
    game_metadata = Column(JSON, nullable=True)

