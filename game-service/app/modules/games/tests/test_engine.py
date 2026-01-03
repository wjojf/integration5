"""
Tests for Game Engine Module
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

from app.modules.game_engine.service import GameEngineService
from app.modules.game_engine.models import Player, Cell, GameStatus, ROWS, COLS


def test_new_game_empty_board():
    """Test creating a new game with empty board."""
    service = GameEngineService()
    state = service.new_game()
    assert state.current_player == Player.P1
    assert state.status == GameStatus.ONGOING
    assert state.move_number == 0
    assert len(state.board) == ROWS
    assert all(len(row) == COLS for row in state.board)
    assert all(cell == Cell.EMPTY for row in state.board for cell in row)


def test_first_move_updates_board_and_player():
    """Test that first move updates board and switches player."""
    service = GameEngineService()
    state = service.new_game(starting_player=Player.P1)
    state2 = service.apply_move(state, 3)

    # P1 should have dropped in column 3, bottom row
    assert state2.board[ROWS - 1][3] == Cell.P1
    # Next player is P2
    assert state2.current_player == Player.P2
    # Move number incremented
    assert state2.move_number == 1
    # Status still ongoing
    assert state2.status == GameStatus.ONGOING


def test_illegal_move_full_column():
    """Test that moving in a full column raises an error."""
    service = GameEngineService()
    state = service.new_game()
    # Fill column 0
    for _ in range(ROWS):
        state = service.apply_move(state, 0)

    # Column 0 should now be full; next move in 0 should raise
    with pytest.raises(ValueError):
        service.apply_move(state, 0)


def test_horizontal_win():
    """Test horizontal win detection."""
    service = GameEngineService()
    state = service.new_game(starting_player=Player.P1)
    # P1 plays columns 0,1,2,3; P2 just plays elsewhere
    state = service.apply_move(state, 0)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 1)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 2)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 3)  # P1 -> horizontal connect 4

    assert state.status == GameStatus.WIN_P1
    assert service.check_winner(state) == Player.P1


def test_vertical_win():
    """Test vertical win detection."""
    service = GameEngineService()
    state = service.new_game(starting_player=Player.P1)
    # P1 plays column 0 four times, P2 elsewhere
    state = service.apply_move(state, 0)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 0)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 0)  # P1
    state = service.apply_move(state, 6)  # P2
    state = service.apply_move(state, 0)  # P1 -> vertical connect 4

    assert state.status == GameStatus.WIN_P1
    assert service.check_winner(state) == Player.P1


def test_draw_state():
    """Test draw detection."""
    service = GameEngineService()
    state = service.new_game()
    # Fill the top row to simulate a full board (no empty cells in top row)
    for c in range(COLS):
        state.board[0][c] = Cell.P1 if c % 2 == 0 else Cell.P2

    # Make sure no winner
    assert service.check_winner(state) is None
    # But top row has no EMPTY → draw from our logic
    assert service.is_draw(state) is True


def test_no_legal_moves_when_game_finished():
    """Test that no legal moves are available when game is finished."""
    service = GameEngineService()
    state = service.new_game()
    # Force a finished status to test behavior
    state.status = GameStatus.WIN_P1
    assert service.get_legal_moves(state) == []
