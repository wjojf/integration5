"""
Custom Types for AI Player Module
Type definitions for clear contracts.
"""
from dataclasses import dataclass
from typing import Optional
from .models import AIPlayerLevel


@dataclass
class MoveEvaluation:
    """Evaluation of a move decision."""
    move_column: int
    confidence: float
    mcts_visit_count: Optional[int]
    mcts_search_depth: Optional[int]
    evaluation_score: float
    thinking_time_ms: float


@dataclass
class DifficultyAdjustmentResult:
    """Result of difficulty adjustment calculation."""
    recommended_level: AIPlayerLevel
    reason: str
    previous_level: AIPlayerLevel
    win_rate: float


