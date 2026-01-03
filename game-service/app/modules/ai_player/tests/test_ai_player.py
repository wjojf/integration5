"""
Tests for AI Player Module
"""
import os
import sys
from pathlib import Path

import pytest

# --- Ensure project root is on sys.path so `import app` works ---

CURRENT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = CURRENT_DIR

# Walk upwards until we find a directory that contains 'app'
while PROJECT_ROOT != PROJECT_ROOT.parent and not (PROJECT_ROOT / "app").is_dir():
    PROJECT_ROOT = PROJECT_ROOT.parent

# Put that directory at the front of sys.path
sys.path.insert(0, str(PROJECT_ROOT))

# --- Now absolute imports from the app package work ---

from app.modules.ai_player.models import AIPlayerLevel, MoveResponse
from app.modules.ai_player.service import AIPlayerService
from app.config import settings


# ============================================================
# PART 1: MULTIPLE SKILL LEVELS (4 Levels)
# ============================================================

def test_ai_player_has_four_skill_levels():
    """Test that AI Player has 4 difficulty levels."""
    levels = list(AIPlayerLevel)
    assert len(levels) == 4
    assert AIPlayerLevel.LOW in levels
    assert AIPlayerLevel.MEDIUM in levels
    assert AIPlayerLevel.HIGH in levels
    assert AIPlayerLevel.VERY_HIGH in levels


def test_skill_levels_have_correct_values():
    """Test that skill levels have correct string values."""
    assert AIPlayerLevel.LOW.value == "low"
    assert AIPlayerLevel.MEDIUM.value == "medium"
    assert AIPlayerLevel.HIGH.value == "high"
    assert AIPlayerLevel.VERY_HIGH.value == "very_high"


def test_service_has_iteration_config():
    """Test that service has iteration counts for each level."""
    service = AIPlayerService()
    
    assert AIPlayerLevel.LOW in service.level_iterations
    assert AIPlayerLevel.MEDIUM in service.level_iterations
    assert AIPlayerLevel.HIGH in service.level_iterations
    assert AIPlayerLevel.VERY_HIGH in service.level_iterations


def test_iteration_counts_increase_with_difficulty():
    """Test that higher levels have more iterations."""
    service = AIPlayerService()
    
    low = service.level_iterations[AIPlayerLevel.LOW]
    medium = service.level_iterations[AIPlayerLevel.MEDIUM]
    high = service.level_iterations[AIPlayerLevel.HIGH]
    very_high = service.level_iterations[AIPlayerLevel.VERY_HIGH]
    
    assert low < medium < high < very_high


def test_iterations_from_config():
    """Test that iterations match config settings."""
    service = AIPlayerService()
    
    assert service.level_iterations[AIPlayerLevel.LOW] == settings.AI_PLAYER_MCTS_ITERATIONS_LOW
    assert service.level_iterations[AIPlayerLevel.MEDIUM] == settings.AI_PLAYER_MCTS_ITERATIONS_MEDIUM
    assert service.level_iterations[AIPlayerLevel.HIGH] == settings.AI_PLAYER_MCTS_ITERATIONS_HIGH
    assert service.level_iterations[AIPlayerLevel.VERY_HIGH] == settings.AI_PLAYER_MCTS_ITERATIONS_VERY_HIGH


# ============================================================
# PART 2: DYNAMIC DIFFICULTY ADJUSTMENT
# ============================================================

def test_adjust_difficulty_increase_when_winning():
    """Player win rate 80% → increase difficulty."""
    service = AIPlayerService()
    
    result = service.adjust_difficulty(
        current_level=AIPlayerLevel.LOW,
        win_rate=0.80,
        target_win_rate=0.5
    )
    
    assert result.recommended_level == AIPlayerLevel.MEDIUM
    assert "Increasing" in result.reason or "high" in result.reason.lower()


def test_adjust_difficulty_decrease_when_losing():
    """Player win rate 20% → decrease difficulty."""
    service = AIPlayerService()
    
    result = service.adjust_difficulty(
        current_level=AIPlayerLevel.HIGH,
        win_rate=0.20,
        target_win_rate=0.5
    )
    
    assert result.recommended_level == AIPlayerLevel.MEDIUM
    assert "Decreasing" in result.reason or "low" in result.reason.lower()


def test_adjust_difficulty_keep_when_balanced():
    """Player win rate 50% → keep same level."""
    service = AIPlayerService()
    
    result = service.adjust_difficulty(
        current_level=AIPlayerLevel.MEDIUM,
        win_rate=0.50,
        target_win_rate=0.5
    )
    
    assert result.recommended_level == AIPlayerLevel.MEDIUM


def test_adjust_difficulty_max_level_cap():
    """Cannot increase beyond VERY_HIGH."""
    service = AIPlayerService()
    
    result = service.adjust_difficulty(
        current_level=AIPlayerLevel.VERY_HIGH,
        win_rate=0.90,
        target_win_rate=0.5
    )
    
    assert result.recommended_level == AIPlayerLevel.VERY_HIGH
    assert "maximum" in result.reason.lower() or result.recommended_level == AIPlayerLevel.VERY_HIGH


def test_adjust_difficulty_min_level_cap():
    """Cannot decrease below LOW."""
    service = AIPlayerService()
    
    result = service.adjust_difficulty(
        current_level=AIPlayerLevel.LOW,
        win_rate=0.10,
        target_win_rate=0.5
    )
    
    assert result.recommended_level == AIPlayerLevel.LOW
    assert "minimum" in result.reason.lower() or result.recommended_level == AIPlayerLevel.LOW


# ============================================================
# PART 3: MOVE RESPONSE MODEL
# ============================================================

def test_move_response_dataclass():
    """Test MoveResponse dataclass creation."""
    response = MoveResponse(
        move_column=3,
        player_level=AIPlayerLevel.MEDIUM,
        confidence=0.75,
        thinking_time_ms=150.0
    )
    
    assert response.move_column == 3
    assert response.player_level == AIPlayerLevel.MEDIUM
    assert response.confidence == 0.75
    assert response.thinking_time_ms == 150.0


def test_move_response_column_range():
    """Test move_column is valid Connect Four column (0-6)."""
    # Valid columns
    for col in range(7):
        response = MoveResponse(
            move_column=col,
            player_level=AIPlayerLevel.LOW
        )
        assert 0 <= response.move_column <= 6


# ============================================================
# PART 4: DTO VALIDATION
# ============================================================

def test_dto_imports():
    """Test that all DTOs can be imported."""
    from app.modules.ai_player.dto import (
        MoveRequest,
        MoveResponse,
        DifficultyRequest,
        DifficultyResponse,
        LevelInfo,
        LevelsResponse,
        SelfPlayRequest,
        SelfPlayResponse,
        GameResult
    )
    
    assert MoveRequest is not None
    assert SelfPlayRequest is not None


def test_self_play_request_defaults():
    """Test SelfPlayRequest has correct defaults."""
    from app.modules.ai_player.dto import SelfPlayRequest
    
    request = SelfPlayRequest()
    
    assert request.num_games == 10
    assert request.player1_level == AIPlayerLevel.MEDIUM
    assert request.player2_level == AIPlayerLevel.MEDIUM
    assert request.epsilon == 0.1
    assert request.log_to_db == True


def test_self_play_request_custom_values():
    """Test SelfPlayRequest with custom values."""
    from app.modules.ai_player.dto import SelfPlayRequest
    
    request = SelfPlayRequest(
        num_games=50,
        player1_level=AIPlayerLevel.LOW,
        player2_level=AIPlayerLevel.HIGH,
        epsilon=0.2,
        log_to_db=False
    )
    
    assert request.num_games == 50
    assert request.player1_level == AIPlayerLevel.LOW
    assert request.player2_level == AIPlayerLevel.HIGH
    assert request.epsilon == 0.2
    assert request.log_to_db == False


# ============================================================
# Run tests
# ============================================================

if __name__ == "__main__":
    pytest.main([__file__, "-v"])
