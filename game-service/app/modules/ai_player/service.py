"""
AI Player Service
MCTS-based AI player implementation with multiple skill levels.
"""
import time
from typing import Optional, Dict, Any

from app.config import settings
from .models import AIPlayerLevel, MoveResponse
from .types import MoveEvaluation, DifficultyAdjustmentResult


class AIPlayerService:
    """Service for AI player moves using MCTS algorithm."""
    
    def __init__(self):
        """Initialize the AI player service."""
        # MCTS iterations per level
        self.level_iterations = {
            AIPlayerLevel.LOW: settings.AI_PLAYER_MCTS_ITERATIONS_LOW,
            AIPlayerLevel.MEDIUM: settings.AI_PLAYER_MCTS_ITERATIONS_MEDIUM,
            AIPlayerLevel.HIGH: settings.AI_PLAYER_MCTS_ITERATIONS_HIGH,
            AIPlayerLevel.VERY_HIGH: settings.AI_PLAYER_MCTS_ITERATIONS_VERY_HIGH,
        }
        self._game_service = None
    
    def _get_game_service(self):
        """Get game service from DI container or create new instance."""
        if self._game_service is not None:
            return self._game_service
        
        # Create new instance directly
        from app.modules.games.games.connect_four.game import ConnectFourGame
        from app.modules.games.services.game_service import GameService
        from app.core.game.factory import GameFactory
        from app.core.game.registry import GameRegistry
        
        registry = GameRegistry()
        registry.register(ConnectFourGame)
        factory = GameFactory(registry)
        self._game_service = GameService(factory)
        return self._game_service
    
    def _parse_game_state(self, game_state: Dict[str, Any]) -> Dict[str, Any]:
        """Parse and validate game state."""
        # Game state should already be a dict from games module
        if not isinstance(game_state, dict):
            raise ValueError(f"Invalid game state type: {type(game_state)}")
        return game_state
    
    def _get_legal_moves(self, game_type: str, state: Dict[str, Any], player_id: str) -> list[Dict[str, Any]]:
        """Get legal moves for the current state."""
        game_service = self._get_game_service()
        legal_moves = game_service.get_legal_moves(
            game_type=game_type,
            game_state=state,
            player_id=player_id
        )
        
        if not legal_moves:
            return []
        
        return legal_moves
    
    def _apply_move(self, game_type: str, state: Dict[str, Any], move: Dict[str, Any], player_id: str) -> Dict[str, Any]:
        """Apply a move to the game state."""
        game_service = self._get_game_service()
        new_state = game_service.apply_move(
            game_type=game_type,
            game_state=state,
            move_data=move,
            player_id=player_id
        )
        return new_state
    
    def _evaluate_position(self, game_type: str, state: Dict[str, Any], player_id: str) -> float:
        """Evaluate position for MCTS (simplified heuristic)."""
        # TODO: Implement proper evaluation using games module
        # For now, return a simple heuristic
        status = self._get_game_status(game_type, state)
        
        if status == "ongoing":
            return 0.0
        
        winner_id = self._get_winner_id(game_type, state)
        if winner_id == player_id:
            return 1.0
        elif winner_id:
            return -1.0
        else:
            return 0.0
    
    def _get_game_status(self, game_type: str, state: Dict[str, Any]) -> str:
        """Get game status."""
        game_service = self._get_game_service()
        return game_service.get_game_status(game_type, state)
    
    def _get_winner_id(self, game_type: str, state: Dict[str, Any]) -> Optional[str]:
        """Get winner ID if game is finished."""
        game_service = self._get_game_service()
        return game_service.get_winner_id(game_type, state)
    
    def _mcts_search(
        self,
        game_type: str,
        state: Dict[str, Any],
        player_id: str,
        iterations: int,
        time_limit: Optional[float] = None
    ) -> Dict[str, Any]:
        """MCTS search algorithm."""
        # TODO: Implement full MCTS algorithm
        # For now, return a random legal move
        legal_moves = self._get_legal_moves(game_type, state, player_id)
        
        if not legal_moves:
            raise ValueError("No legal moves available")
        
        # Simple selection: return first legal move
        # In full implementation, this would use MCTS tree search
        return legal_moves[0]
    
    def get_move(
        self,
        game_type: str,
        game_state: Dict[str, Any],
        player_id: str,
        level: AIPlayerLevel = AIPlayerLevel.MEDIUM,
        time_limit: Optional[float] = None
    ) -> MoveResponse:
        """
        Get AI move for given game state.
        
        Args:
            game_type: Type of game (e.g., 'connect_four')
            game_state: Current game state as dictionary
            player_id: ID of the AI player
            level: AI difficulty level
            time_limit: Maximum time to spend on move (seconds)
            
        Returns:
            MoveResponse with selected move
        """
        try:
            from .mcts import MCTSAlgorithm
            
            iterations = self.level_iterations[level]
            game_service = self._get_game_service()
            
            # Use MCTS algorithm
            mcts = MCTSAlgorithm(game_service, game_type=game_type)
            result = mcts.search(game_state, player_id, iterations=iterations)
            
            return MoveResponse(
                move=result.best_move,
                confidence=result.win_rate,
                evaluation=result.win_rate,
                time_taken=result.thinking_time_ms / 1000.0,
                iterations=result.visits
            )
        except Exception as e:
            raise ValueError(f"Failed to get AI move: {str(e)}")
    
    def adjust_difficulty(
        self,
        current_level: AIPlayerLevel,
        win_rate: float,
        target_win_rate: float = 0.5
    ) -> DifficultyAdjustmentResult:
        """
        Adjust AI difficulty based on win rate.
        
        Args:
            current_level: Current AI difficulty level
            win_rate: Current win rate (0.0 to 1.0)
            target_win_rate: Target win rate (default 0.5)
            
        Returns:
            DifficultyAdjustmentResult with new level and reason
        """
        threshold = 0.1
        
        if win_rate > target_win_rate + threshold:
            # Player winning too much, increase difficulty
            if current_level == AIPlayerLevel.LOW:
                new_level = AIPlayerLevel.MEDIUM
                reason = "Increasing difficulty: player win rate too high"
            elif current_level == AIPlayerLevel.MEDIUM:
                new_level = AIPlayerLevel.HIGH
                reason = "Increasing difficulty: player win rate too high"
            elif current_level == AIPlayerLevel.HIGH:
                new_level = AIPlayerLevel.VERY_HIGH
                reason = "Increasing difficulty: player win rate too high"
            else:
                new_level = current_level
                reason = "Already at maximum difficulty"
        elif win_rate < target_win_rate - threshold:
            # Player losing too much, decrease difficulty
            if current_level == AIPlayerLevel.VERY_HIGH:
                new_level = AIPlayerLevel.HIGH
                reason = "Decreasing difficulty: player win rate too low"
            elif current_level == AIPlayerLevel.HIGH:
                new_level = AIPlayerLevel.MEDIUM
                reason = "Decreasing difficulty: player win rate too low"
            elif current_level == AIPlayerLevel.MEDIUM:
                new_level = AIPlayerLevel.LOW
                reason = "Decreasing difficulty: player win rate too low"
            else:
                new_level = current_level
                reason = "Already at minimum difficulty"
        else:
            new_level = current_level
            reason = "Win rate within acceptable range"
        
        return DifficultyAdjustmentResult(
            recommended_level=new_level,
            previous_level=current_level,
            reason=reason,
            win_rate=win_rate
        )
