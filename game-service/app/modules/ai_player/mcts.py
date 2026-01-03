"""
Monte Carlo Tree Search (MCTS) - Custom Implementation

Simple MCTS algorithm with UCB1 for Connect Four.
No external dependencies - easy to understand and explain.

Algorithm Steps:
1. Selection   - Pick best child using UCB1
2. Expansion   - Add new child node
3. Simulation  - Random playout until game ends
4. Backpropagation - Update win/visit counts

UCB1 Formula:
    UCB1 = win_rate + C * sqrt(ln(parent_visits) / child_visits)
    
    - win_rate: How often we win from this node
    - C: Exploration constant (√2 ≈ 1.414 is standard)
    - Higher UCB1 = more promising or under-explored
"""
import math
import random
import time
from dataclasses import dataclass
from typing import Optional, List, Dict, Any, TYPE_CHECKING

if TYPE_CHECKING:
    from app.modules.games.services.game_service import GameService


# Game status constants
STATUS_ONGOING = "ongoing"
STATUS_WIN_P1 = "win_player_1"
STATUS_WIN_P2 = "win_player_2"
STATUS_DRAW = "draw"


@dataclass
class MCTSResult:
    """Result from MCTS search."""
    best_move: Dict[str, Any]       # Best move to play (e.g., {"column": 3})
    visits: int                     # Total simulations run
    win_rate: float                 # Win probability of best move
    move_scores: Dict[int, float]   # Win rate for each move index
    move_visits: Dict[int, int]     # Visit count for each move index
    thinking_time_ms: float         # Time spent searching


class MCTSNode:
    """
    A node in the MCTS tree.
    
    Each node represents a game state after a move.
    Tracks wins and visits for UCB1 calculation.
    """
    
    def __init__(
        self,
        state: Dict[str, Any],
        parent: 'MCTSNode' = None,
        move: Dict[str, Any] = None,
        move_idx: int = None
    ):
        self.state = state          # Game state at this node (dict)
        self.parent = parent        # Parent node (None for root)
        self.move = move            # Move that led to this state
        self.move_idx = move_idx    # Index in legal moves list
        
        self.children: List[MCTSNode] = []  # Child nodes
        self.untried_moves: List[Dict[str, Any]] = []  # Moves not yet expanded
        self.untried_indices: List[int] = []  # Indices of untried moves
        
        self.wins = 0.0     # Number of wins from this node
        self.visits = 0     # Number of times visited
        
    @property
    def win_rate(self) -> float:
        """Calculate win rate (0 to 1)."""
        if self.visits == 0:
            return 0.0
        return self.wins / self.visits
    
    def ucb1(self, exploration: float = 1.414) -> float:
        """
        Calculate UCB1 value for node selection.
        
        UCB1 = win_rate + C * sqrt(ln(parent_visits) / visits)
        
        Args:
            exploration: C constant (default √2)
        
        Returns:
            UCB1 value (higher = more promising)
        """
        if self.visits == 0:
            return float('inf')  # Unvisited nodes have highest priority
        
        exploitation = self.win_rate
        exploration_term = exploration * math.sqrt(
            math.log(self.parent.visits) / self.visits
        )
        
        return exploitation + exploration_term


class MCTSAlgorithm:
    """
    Monte Carlo Tree Search for Connect Four.
    
    Simple implementation:
    - UCB1 for selection
    - Random playouts for simulation
    - Full backpropagation
    
    Usage:
        mcts = MCTSAlgorithm.from_container()
        result = mcts.search(game_state, player_id, iterations=500)
        best_move = result.best_move
    """
    
    def __init__(
        self,
        game_service: "GameService",
        game_type: str = "connect_four",
        exploration: float = 1.414
    ):
        """
        Initialize MCTS.
        
        Args:
            game_service: GameService from games module
            game_type: Type of game (default: connect_four)
            exploration: UCB1 exploration constant (√2 standard)
        """
        self.game_service = game_service
        self.game_type = game_type
        self.exploration = exploration
    
    @classmethod
    def from_container(cls, game_type: str = "connect_four", exploration: float = 1.414) -> "MCTSAlgorithm":
        """
        Create MCTSAlgorithm using GameService from DI container.
        
        This is the preferred way to create an MCTSAlgorithm instance
        when the games module is properly initialized.
        """
        from app.shared.container import container
        from app.modules.games.services.game_service import GameService
        
        if not container.has(GameService):
            raise RuntimeError(
                "GameService not registered in container. "
                "Ensure games module is initialized first."
            )
        
        game_service = container.get(GameService)
        return cls(game_service, game_type, exploration)
    
    def search(
        self,
        state: Dict[str, Any],
        player_id: str,
        iterations: int = 500
    ) -> MCTSResult:
        """
        Run MCTS search.
        
        Args:
            state: Current game state (dict)
            player_id: ID of the player making the move
            iterations: Number of simulations to run
        
        Returns:
            MCTSResult with best move and statistics
        """
        start_time = time.time()
        
        # Create root node
        root = MCTSNode(state)
        legal_moves = self.game_service.get_legal_moves(self.game_type, state, player_id)
        root.untried_moves = legal_moves
        root.untried_indices = list(range(len(legal_moves)))
        
        # Store player_id for simulation
        self._current_player_id = player_id
        
        # Handle edge cases
        if not legal_moves:
            return MCTSResult(
                best_move={},
                visits=0,
                win_rate=0.0,
                move_scores={},
                move_visits={},
                thinking_time_ms=0.0
            )
        
        if len(legal_moves) == 1:
            return MCTSResult(
                best_move=legal_moves[0],
                visits=1,
                win_rate=0.5,
                move_scores={0: 0.5},
                move_visits={0: 1},
                thinking_time_ms=0.0
            )
        
        # Run iterations
        for _ in range(iterations):
            node = root
            current_player = player_id
            
            # 1. SELECTION - Pick best child until we find unexpanded node
            while not node.untried_moves and node.children:
                node = self._select_child(node)
                # Track player alternation
                current_player = self._get_opponent(current_player, state)
            
            # 2. EXPANSION - Add one new child
            if node.untried_moves:
                node = self._expand(node, current_player)
                current_player = self._get_opponent(current_player, state)
            
            # 3. SIMULATION - Random playout
            result = self._simulate(node.state, player_id)
            
            # 4. BACKPROPAGATION - Update wins/visits
            self._backpropagate(node, result)
        
        # Find best move (most visited child)
        if not root.children:
            return MCTSResult(
                best_move=legal_moves[0] if legal_moves else {},
                visits=root.visits,
                win_rate=0.5,
                move_scores={},
                move_visits={},
                thinking_time_ms=(time.time() - start_time) * 1000
            )
        
        best_child = max(root.children, key=lambda c: c.visits)
        
        # Calculate move scores and visit counts
        move_scores = {}
        move_visits = {}
        for child in root.children:
            if child.move_idx is not None:
                move_scores[child.move_idx] = child.win_rate
                move_visits[child.move_idx] = child.visits
        
        elapsed = (time.time() - start_time) * 1000
        
        return MCTSResult(
            best_move=best_child.move,
            visits=root.visits,
            win_rate=best_child.win_rate,
            move_scores=move_scores,
            move_visits=move_visits,
            thinking_time_ms=elapsed
        )
    
    def _get_opponent(self, player_id: str, state: Dict[str, Any]) -> str:
        """Get opponent player ID."""
        player_ids = state.get("player_ids", [])
        if len(player_ids) == 2:
            return player_ids[1] if player_id == player_ids[0] else player_ids[0]
        return player_id
    
    def _select_child(self, node: MCTSNode) -> MCTSNode:
        """Select child with highest UCB1 value."""
        return max(node.children, key=lambda c: c.ucb1(self.exploration))
    
    def _expand(self, node: MCTSNode, current_player: str) -> MCTSNode:
        """Expand one untried move."""
        # Pick random untried move
        idx = random.randint(0, len(node.untried_moves) - 1)
        move = node.untried_moves.pop(idx)
        move_idx = node.untried_indices.pop(idx)
        
        # Create new state
        new_state = self.game_service.apply_move(
            self.game_type, node.state, move, current_player
        )
        
        # Create child node
        child = MCTSNode(new_state, parent=node, move=move, move_idx=move_idx)
        
        # Get legal moves for next player
        next_player = self._get_opponent(current_player, new_state)
        legal_moves = self.game_service.get_legal_moves(
            self.game_type, new_state, next_player
        )
        child.untried_moves = legal_moves
        child.untried_indices = list(range(len(legal_moves)))
        
        node.children.append(child)
        return child
    
    def _simulate(self, state: Dict[str, Any], original_player: str) -> int:
        """
        Random playout until game ends.
        
        Returns:
            1 if original player wins
            0 if draw
           -1 if original player loses
        """
        current_state = state.copy()
        max_moves = 42  # Connect Four max
        
        # Get initial current player
        current_player = self.game_service.get_current_player_id(
            self.game_type, current_state
        )
        
        for _ in range(max_moves):
            status = self.game_service.get_game_status(self.game_type, current_state)
            
            if status != STATUS_ONGOING:
                winner = self.game_service.get_winner_id(self.game_type, current_state)
                if winner is None:
                    return 0  # Draw
                elif winner == original_player:
                    return 1  # Win
                else:
                    return -1  # Loss
            
            legal_moves = self.game_service.get_legal_moves(
                self.game_type, current_state, current_player
            )
            
            if not legal_moves:
                return 0  # Draw
            
            move = random.choice(legal_moves)
            current_state = self.game_service.apply_move(
                self.game_type, current_state, move, current_player
            )
            
            # Switch player
            current_player = self._get_opponent(current_player, current_state)
        
        return 0  # Default to draw
    
    def _backpropagate(self, node: MCTSNode, result: int):
        """
        Update win/visit counts up the tree.
        
        Alternates perspective at each level since
        players take turns.
        """
        current = node
        current_result = result
        
        while current is not None:
            current.visits += 1
            
            # Win = +1, Draw = +0.5, Loss = +0
            if current_result == 1:
                current.wins += 1
            elif current_result == 0:
                current.wins += 0.5
            
            # Flip result for parent (opponent's perspective)
            current_result = -current_result
            current = current.parent
