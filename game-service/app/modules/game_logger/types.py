"""
Custom Types for Game Logger Module
Type definitions for clear contracts and deterministic behavior.
"""
from dataclasses import dataclass
from typing import Optional, Dict, Any, List
from pathlib import Path


@dataclass
class GameLogRow:
    """Structured representation of a game log row for dataset export (ML-focused)."""
    # Move identifiers
    id: int
    session_id: str
    move_number: int

    # Player info
    current_player: str  # 'p1' or 'p2'
    current_player_id: int  # 1 or 2

    # Board state
    board_state: Dict[str, Any]  # Raw board as JSON
    board_flat: List[int]  # Flattened board [42 elements]
    legal_moves_mask: List[int]  # Binary mask [7 elements]

    # Move played
    played_move: int  # Column 0-6

    # Expert labels (ML training targets)
    expert_policy: List[float]  # MCTS visit distribution [7 elements]
    expert_best_move: int  # Best move from MCTS
    expert_value: float  # Win probability from MCTS

    # Game outcome
    final_result: Optional[str]  # 'win', 'loss', 'draw', 'ongoing'
    final_result_numeric: Optional[float]  # 1.0, 0.0, 0.5

    # MCTS metadata
    mcts_iterations: Optional[int]
    game_status: Optional[str]  # 'ongoing' or 'finished'
    timestamp: str

    # Session metadata (from SessionLogModel)
    game_type: Optional[str]
    p1_type: Optional[str]  # 'human' or 'ai'
    p2_type: Optional[str]
    p1_agent: Optional[str]  # 'mcts_500', etc.
    p2_agent: Optional[str]
    p1_agent_level: Optional[str]  # 'low', 'medium', 'high'
    p2_agent_level: Optional[str]
    winner: Optional[str]  # 'p1', 'p2', 'draw'
    num_moves: Optional[int]
    duration_seconds: Optional[float]
    tag: Optional[str]  # 'selfplay_v1', etc.


@dataclass
class DatasetVersionInfo:
    """Information about a dataset version."""
    version: str
    file_path: Path
    total_records: int
    unique_sessions: int
    unique_tags: int
    agent_types: Dict[str, int]
    date_range_min: Optional[str]
    date_range_max: Optional[str]


@dataclass
class DatasetStats:
    """Statistics for a dataset version."""
    version: str
    file: str
    total_records: int
    unique_sessions: int
    unique_tags: int
    agent_distribution: Dict[str, int]
    outcome_distribution: Dict[str, int]
    date_range: Dict[str, Optional[str]]


@dataclass
class DatasetExportResult:
    """Result of dataset export operation."""
    version: str
    file_path: Path
    file_size_mb: float
    total_records: int
    dvc_file: Optional[str] = None


@dataclass
class DVCConfig:
    """DVC configuration."""
    remote_type: str
    remote_url: str
    remote_name: str = "default"


@dataclass
class DVCInitResult:
    """Result of DVC initialization."""
    initialized: bool
    message: str
    config: Optional[DVCConfig] = None


@dataclass
class DatasetInfo:
    """Information about a tracked dataset."""
    path: str
    dvc_file: str
    size_bytes: int
    size_mb: float
    exists: bool
