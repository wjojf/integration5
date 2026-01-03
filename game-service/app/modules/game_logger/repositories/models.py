"""
SQLAlchemy ORM models for ML-focused game logging.
Optimized for Connect Four training data with expert labels.
"""
from datetime import datetime

from sqlalchemy import Column, String, Integer, DateTime, Float, BigInteger, ForeignKey, ARRAY
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import declarative_base, relationship

Base = declarative_base()


class SessionLogModel(Base):
    """ML-focused game session metadata."""
    __tablename__ = "session_logs"

    # Primary Key
    session_id = Column(String(64), primary_key=True)
    game_type = Column(String(32), nullable=False, default='connect_four', index=True)

    # Player Configuration
    p1_type = Column(String(16), nullable=False)  # 'human' | 'ai'
    p2_type = Column(String(16), nullable=False)  # 'human' | 'ai'
    p1_agent = Column(String(64), nullable=True, index=True)  # 'mcts_100', 'mcts_500', etc.
    p2_agent = Column(String(64), nullable=True, index=True)
    p1_agent_level = Column(String(16), nullable=True)  # 'low', 'medium', 'high'
    p2_agent_level = Column(String(16), nullable=True)

    # Match Results
    winner = Column(String(8), nullable=True, index=True)  # 'p1', 'p2', 'draw'
    num_moves = Column(Integer, nullable=False, default=0)
    duration_seconds = Column(Float, nullable=True)

    # Timestamps
    start_time = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    end_time = Column(DateTime, nullable=True)

    # Dataset Management
    tag = Column(String(128), nullable=True, index=True)  # 'selfplay_v1', 'dataset_v2', etc.

    # Relationship to game moves
    moves = relationship("GameMoveModel", back_populates="session", cascade="all, delete-orphan")


class GameMoveModel(Base):
    """ML training data - individual moves with expert labels."""
    __tablename__ = "game_moves"

    # Primary Key & Foreign Key
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    session_id = Column(
        String(64),
        ForeignKey('session_logs.session_id', ondelete='CASCADE'),
        nullable=False,
        index=True
    )
    move_number = Column(Integer, nullable=False, index=True)

    # Player Info
    current_player = Column(String(8), nullable=False)  # 'p1' | 'p2'
    current_player_id = Column(Integer, nullable=False, index=True) # 1 | 2

    # Board State
    board_state = Column(JSONB, nullable=False)  # Raw JSON board
    board_flat = Column(ARRAY(Integer, dimensions=1), nullable=False)  # [42] flattened board

    # Legal Moves
    legal_moves_mask = Column(ARRAY(Integer, dimensions=1), nullable=False)  # [7] binary mask

    # Move Played
    played_move = Column(Integer, nullable=False)  # 0-6

    # Expert/MCTS Labels (CRITICAL FOR ML TRAINING)
    expert_policy = Column(ARRAY(Float, dimensions=1), nullable=False)  # [7] probabilities
    expert_best_move = Column(Integer, nullable=False)  # argmax(expert_policy)
    expert_value = Column(Float, nullable=False)  # win probability [0-1]

    # Game Result
    final_result = Column(String(16), nullable=True)  # 'win', 'loss', 'draw', 'ongoing'
    final_result_numeric = Column(Float, nullable=True)  # 1.0 / 0.0 / 0.5

    # Metadata
    mcts_iterations = Column(Integer, nullable=True)
    game_status = Column(String(16), nullable=True, index=True)  # 'ongoing', 'finished'
    timestamp = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)

    # Relationship to session
    session = relationship("SessionLogModel", back_populates="moves")

