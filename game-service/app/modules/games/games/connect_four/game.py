"""
Connect Four game implementation.
"""
from typing import List, Optional, Dict, Any

from app.core.game.base import BaseGame
from app.core.game.interfaces import GameStateInterface, GameMoveInterface
from .models import ConnectFourState, ConnectFourMove


class ConnectFourGame(BaseGame):
    """Connect Four game implementation."""
    
    def __init__(self) -> None:
        super().__init__("connect_four")
        self.ROWS = 6
        self.COLS = 7
    
    def create_initial_state(
        self,
        player_ids: List[str],
        starting_player_id: str,
        configuration: Optional[Dict[str, Any]] = None
    ) -> ConnectFourState:
        """Create initial Connect Four state."""
        if len(player_ids) != 2:
            raise ValueError("Connect Four requires exactly 2 players")
        
        # Validate player IDs are not empty (works for both human and AI players)
        for player_id in player_ids:
            if not player_id or not player_id.strip():
                raise ValueError(f"Player ID cannot be empty or whitespace-only")
        
        if not starting_player_id or not starting_player_id.strip():
            raise ValueError("Starting player ID must be non-empty")
        
        if starting_player_id not in player_ids:
            raise ValueError(f"Starting player {starting_player_id} not in player list")
        
        board = [[0] * self.COLS for _ in range(self.ROWS)]
        
        return ConnectFourState(
            board=board,
            current_player_id=starting_player_id,
            player_ids=player_ids,
            move_number=0,
            game_type="connect_four"
        )
    
    def apply_move(
        self,
        state: GameStateInterface,
        move: GameMoveInterface,
        player_id: str
    ) -> ConnectFourState:
        """Apply a move to the Connect Four state."""
        if not isinstance(state, ConnectFourState):
            raise ValueError("Invalid state type for Connect Four")
        if not isinstance(move, ConnectFourMove):
            raise ValueError("Invalid move type for Connect Four")
        
        if state.current_player_id != player_id:
            raise ValueError(f"It's not player {player_id}'s turn")
        
        column = move.column
        if column < 0 or column >= self.COLS:
            raise ValueError(f"Invalid column: {column}")
        
        # Find the lowest empty row in the column
        board = [row[:] for row in state.board]  # Deep copy
        
        # Determine player number (1 or 2) based on player_id position
        if player_id not in state.player_ids:
            raise ValueError(f"Player {player_id} not in game")
        
        player_index = state.player_ids.index(player_id)
        player_num = player_index + 1  # 1 for first player, 2 for second
        
        row = None
        for r in range(self.ROWS - 1, -1, -1):
            if board[r][column] == 0:
                board[r][column] = player_num
                row = r
                break
        
        if row is None:
            raise ValueError(f"Column {column} is full")
        
        # Determine next player - alternate
        next_player_index = (player_index + 1) % len(state.player_ids)
        next_player_id = state.player_ids[next_player_index]
        
        new_state = ConnectFourState(
            board=board,
            current_player_id=next_player_id,
            player_ids=state.player_ids,
            move_number=state.move_number + 1,
            game_type="connect_four"
        )
        
        return new_state
    
    def get_legal_moves(
        self,
        state: GameStateInterface,
        player_id: str
    ) -> List[ConnectFourMove]:
        """Get legal moves for Connect Four."""
        if not isinstance(state, ConnectFourState):
            raise ValueError("Invalid state type for Connect Four")
        
        legal_moves = []
        for col in range(self.COLS):
            # Check if column has space
            if state.board[0][col] == 0:
                legal_moves.append(ConnectFourMove(column=col))
        
        return legal_moves
    
    def get_game_status(self, state: GameStateInterface) -> str:
        """Get game status."""
        if not isinstance(state, ConnectFourState):
            raise ValueError("Invalid state type for Connect Four")
        
        # Check for winner
        winner = self._check_winner(state.board)
        if winner:
            return f"win_p{winner}"
        
        # Check for draw
        if self._is_board_full(state.board):
            return "draw"
        
        return "ongoing"
    
    def get_winner_id(self, state: GameStateInterface) -> Optional[str]:
        """Get winner ID."""
        if not isinstance(state, ConnectFourState):
            raise ValueError("Invalid state type for Connect Four")
        
        status = self.get_game_status(state)
        if status.startswith("win_p"):
            winner_num = int(status.split("_")[1].replace("p", ""))
            if winner_num <= len(state.player_ids):
                return state.player_ids[winner_num - 1]
        return None
    
    def get_current_player_id(self, state: GameStateInterface) -> str:
        """Get current player ID."""
        if not isinstance(state, ConnectFourState):
            raise ValueError("Invalid state type for Connect Four")
        return state.current_player_id
    
    def _check_winner(self, board: List[List[int]]) -> Optional[int]:
        """Check for a winner (returns 1 or 2, or None)."""
        # Check horizontal
        for row in range(self.ROWS):
            for col in range(self.COLS - 3):
                if (board[row][col] != 0 and
                    board[row][col] == board[row][col+1] ==
                    board[row][col+2] == board[row][col+3]):
                    return board[row][col]
        
        # Check vertical
        for row in range(self.ROWS - 3):
            for col in range(self.COLS):
                if (board[row][col] != 0 and
                    board[row][col] == board[row+1][col] ==
                    board[row+2][col] == board[row+3][col]):
                    return board[row][col]
        
        # Check diagonal (down-right)
        for row in range(self.ROWS - 3):
            for col in range(self.COLS - 3):
                if (board[row][col] != 0 and
                    board[row][col] == board[row+1][col+1] ==
                    board[row+2][col+2] == board[row+3][col+3]):
                    return board[row][col]
        
        # Check diagonal (down-left)
        for row in range(self.ROWS - 3):
            for col in range(3, self.COLS):
                if (board[row][col] != 0 and
                    board[row][col] == board[row+1][col-1] ==
                    board[row+2][col-2] == board[row+3][col-3]):
                    return board[row][col]
        
        return None
    
    def _is_board_full(self, board: List[List[int]]) -> bool:
        """Check if board is full."""
        return all(board[0][col] != 0 for col in range(self.COLS))

