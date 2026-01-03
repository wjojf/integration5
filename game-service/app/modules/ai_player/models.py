"""
AI Player Models
Simple domain models for AI player.
"""
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Dict, Any


class AIPlayerLevel(str, Enum):
    """
    AI difficulty levels - 4 levels.
    
    LOW:       100 iterations  - Beginner (fast, makes mistakes)
    MEDIUM:    500 iterations  - Intermediate (balanced)
    HIGH:      2000 iterations - Advanced (strong, slower)
    VERY_HIGH: 5000 iterations - Expert (very strong, slowest)
    """
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    VERY_HIGH = "very_high"


@dataclass
class MoveResponse:
    """AI's move decision - internal response from service."""
    move: Dict[str, Any]          # Move data (e.g., {"column": 3})
    confidence: float = 0.0       # How confident AI is (0-1)
    evaluation: float = 0.0       # Position evaluation
    time_taken: float = 0.0       # Time taken in seconds
    iterations: int = 0           # MCTS iterations used

