"""
Game service for stateless game operations.
"""
from typing import Dict, Any, List, Optional

from app.core.game.factory import GameFactory
from app.core.game.interfaces import GameInterface, GameStateInterface, GameMoveInterface


class GameService:
    """Service for stateless game operations."""

    def __init__(self, game_factory: GameFactory) -> None:
        self.game_factory = game_factory

    def list_available_games(self) -> Dict[str, str]:
        """List all available games."""
        return self.game_factory.list_available_games()

    def create_initial_state(
        self,
        game_type: str,
        player_ids: List[str],
        starting_player_id: str,
        configuration: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Create initial game state."""
        game = self.game_factory.create_game(game_type)
        state = game.create_initial_state(
            player_ids=player_ids,
            starting_player_id=starting_player_id,
            configuration=configuration or {}
        )
        return state.to_dict()

    def apply_move(
        self,
        game_type: str,
        game_state: Dict[str, Any],
        move_data: Dict[str, Any],
        player_id: str
    ) -> Dict[str, Any]:
        """Apply a move to a game state."""
        game = self.game_factory.create_game(game_type)
        state = self._deserialize_state(game_type, game_state, game)
        move = self._create_move_from_data(game_type, move_data)
        new_state = game.apply_move(state, move, player_id)
        return new_state.to_dict()

    def get_legal_moves(
        self,
        game_type: str,
        game_state: Dict[str, Any],
        player_id: str
    ) -> List[Dict[str, Any]]:
        """Get legal moves for a player."""
        game = self.game_factory.create_game(game_type)
        state = self._deserialize_state(game_type, game_state, game)
        legal_moves = game.get_legal_moves(state, player_id)
        return [move.to_dict() for move in legal_moves]

    def get_game_status(self, game_type: str, game_state: Dict[str, Any]) -> str:
        """Get game status."""
        game = self.game_factory.create_game(game_type)
        state = self._deserialize_state(game_type, game_state, game)
        return game.get_game_status(state)

    def get_winner_id(self, game_type: str, game_state: Dict[str, Any]) -> Optional[str]:
        """Get winner ID if game is finished."""
        game = self.game_factory.create_game(game_type)
        state = self._deserialize_state(game_type, game_state, game)
        return game.get_winner_id(state)

    def get_current_player_id(self, game_type: str, game_state: Dict[str, Any]) -> str:
        """Get current player ID."""
        game = self.game_factory.create_game(game_type)
        state = self._deserialize_state(game_type, game_state, game)
        return game.get_current_player_id(state)

    def _deserialize_state(
        self,
        game_type: str,
        state_dict: Dict[str, Any],
        game: GameInterface
    ) -> GameStateInterface:
        """Deserialize game state from dictionary."""
        if game_type == "connect_four":
            from app.modules.games.games.connect_four.models import ConnectFourState
            return ConnectFourState.from_dict(state_dict)
        raise ValueError(f"Unknown game type: {game_type}")

    def _create_move_from_data(self, game_type: str, move_data: Dict[str, Any]) -> GameMoveInterface:
        """Create move object from dictionary."""
        if game_type == "connect_four":
            from app.modules.games.games.connect_four.models import ConnectFourMove
            column = move_data.get("column")
            if column is None:
                raise ValueError("Missing 'column' in move data for Connect Four")
            return ConnectFourMove(column=column)
        raise ValueError(f"Unknown game type: {game_type}")










