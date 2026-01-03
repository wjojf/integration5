"""
Game engine API router.
Provides game-agnostic APIs for managing games and sessions.
"""
from fastapi import APIRouter, HTTPException, Body
from typing import Dict, Any, List

from app.shared.exceptions import ValidationError
from ..services.game_service import GameService
from ..services.session_service import GameSessionService
from .dto import (
    CreateGameRequest,
    CreateGameResponse,
    CreateSessionRequest,
    CreateSessionResponse,
    GetSessionResponse,
    ApplyMoveRequest,
    ApplyMoveResponse,
    ApplySessionMoveRequest,
    ApplySessionMoveResponse,
    GetLegalMovesRequest,
    GetLegalMovesResponse,
    EvaluateGameRequest,
    EvaluateGameResponse,
    ListGamesResponse,
    GetGameDetailResponse,
    GameInfo,
    AbandonSessionRequest,
    AbandonSessionResponse,
)


# Static game information (no database storage needed)
GAMES_INFO = {
    "connect_four": GameInfo(
        id="550e8400-e29b-41d4-a716-446655440001",
        title="Connect Four",
        genre="Strategy",
        image="https://thumbs.dreamstime.com/b/blue-board-game-connect-four-showing-winning-strategy-red-yellow-pieces-325180667.jpg"
    ),
    "chess": GameInfo(
        id="550e8400-e29b-41d4-a716-446655440002",
        title="Chess",
        genre="Strategy",
        image="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYigKOHenUTuER6t1jBye1G_D1q8IuOauFSQ&s"
    ),
}


def create_router(
    game_service: GameService,
    session_service: GameSessionService
) -> APIRouter:
    """Create game engine router."""
    router = APIRouter(tags=["games"])
    
    @router.get("/games/games", response_model=ListGamesResponse)
    async def list_games() -> ListGamesResponse:
        """List all available games with metadata."""
        # Get available game types from service
        available_game_types = game_service.list_available_games()
        
        # Build list of game info for available games
        games_list = []
        for game_type in available_game_types.keys():
            if game_type in GAMES_INFO:
                games_list.append(GAMES_INFO[game_type])
        
        # Always include chess (external game)
        if "chess" not in available_game_types and "chess" in GAMES_INFO:
            games_list.append(GAMES_INFO["chess"])
        
        return ListGamesResponse(games=games_list)
    
    @router.get("/games/{game_id}", response_model=GetGameDetailResponse)
    async def get_game_detail(game_id: str) -> GetGameDetailResponse:
        """Get detailed information about a specific game."""
        # Find game by ID
        game_info = None
        for game_type, info in GAMES_INFO.items():
            if info.id == game_id:
                game_info = info
                break

        if not game_info:
            raise HTTPException(status_code=404, detail=f"Game with id {game_id} not found")
        
        return GetGameDetailResponse(
            id=game_info.id,
            title=game_info.title,
            genre=game_info.genre,
            image=game_info.image
        )
    
    @router.post("/games/games/create", response_model=CreateGameResponse)
    async def create_game(request: CreateGameRequest) -> CreateGameResponse:
        """Create a new game (stateless)."""
        try:
            game_state = game_service.create_initial_state(
                game_type=request.game_type,
                player_ids=request.player_ids,
                starting_player_id=request.starting_player_id,
                configuration=request.configuration
            )
            
            current_player_id = game_state.get("current_player_id", request.starting_player_id)
            legal_moves = game_service.get_legal_moves(
                game_type=request.game_type,
                game_state=game_state,
                player_id=current_player_id
            )
            status = game_service.get_game_status(request.game_type, game_state)
            
            return CreateGameResponse(
                game_type=request.game_type,
                game_state=game_state,
                legal_moves=legal_moves,
                current_player_id=current_player_id,
                status=status
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to create game: {str(e)}")
    
    @router.post("/games/sessions", response_model=CreateSessionResponse)
    async def create_session(request: CreateSessionRequest) -> CreateSessionResponse:
        """Create a new game session (persisted)."""
        try:
            session = session_service.create_session(
                session_id=request.session_id,
                game_id=request.game_id,
                game_type=request.game_type,
                lobby_id=request.lobby_id,
                player_ids=request.player_ids,
                starting_player_id=request.starting_player_id,
                configuration=request.configuration
            )
            
            return CreateSessionResponse(
                session_id=session.session_id,
                game_id=session.game_id,
                game_type=session.game_type,
                status=session.status.value,
                current_player_id=session.current_player_id,
                player_ids=session.player_ids,
                game_state=session.game_state,
                total_moves=session.total_moves
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to create session: {str(e)}")
    
    @router.get("/games/sessions/{session_id}", response_model=GetSessionResponse)
    async def get_session(session_id: str) -> GetSessionResponse:
        """Get a game session by ID."""
        session = session_service.get_session(session_id)
        if not session:
            raise HTTPException(status_code=404, detail="Session not found")
        
        return GetSessionResponse(
            session_id=session.session_id,
            game_id=session.game_id,
            game_type=session.game_type,
            status=session.status.value,
            current_player_id=session.current_player_id,
            player_ids=session.player_ids,
            game_state=session.game_state,
            total_moves=session.total_moves,
            winner_id=session.winner_id
        )
    
    @router.post("/games/sessions/{session_id}/moves", response_model=ApplySessionMoveResponse)
    async def apply_session_move(
        session_id: str,
        request: ApplySessionMoveRequest
    ) -> ApplySessionMoveResponse:
        """Apply a move to a game session."""
        try:
            session = session_service.apply_move(
                session_id=session_id,
                player_id=request.player_id,
                move_data=request.move
            )
            
            return ApplySessionMoveResponse(
                session_id=session.session_id,
                game_state=session.game_state,
                status=session.status.value,
                current_player_id=session.current_player_id,
                winner_id=session.winner_id,
                total_moves=session.total_moves
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to apply move: {str(e)}")
    
    @router.post("/games/games/move", response_model=ApplyMoveResponse)
    async def apply_move(request: ApplyMoveRequest) -> ApplyMoveResponse:
        """Apply a move to a game (stateless)."""
        try:
            new_state = game_service.apply_move(
                game_type=request.game_type,
                game_state=request.game_state,
                move_data=request.move,
                player_id=request.player_id
            )
            
            current_player_id = game_service.get_current_player_id(request.game_type, new_state)
            legal_moves = game_service.get_legal_moves(
                game_type=request.game_type,
                game_state=new_state,
                player_id=current_player_id
            )
            status = game_service.get_game_status(request.game_type, new_state)
            winner_id = game_service.get_winner_id(request.game_type, new_state)
            
            return ApplyMoveResponse(
                game_type=request.game_type,
                game_state=new_state,
                legal_moves=legal_moves,
                status=status,
                current_player_id=current_player_id,
                winner_id=winner_id
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to apply move: {str(e)}")
    
    @router.post("/games/games/legal-moves", response_model=GetLegalMovesResponse)
    async def get_legal_moves(request: GetLegalMovesRequest) -> GetLegalMovesResponse:
        """Get legal moves for a player."""
        try:
            legal_moves = game_service.get_legal_moves(
                game_type=request.game_type,
                game_state=request.game_state,
                player_id=request.player_id
            )
            return GetLegalMovesResponse(legal_moves=legal_moves)
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get legal moves: {str(e)}")
    
    @router.post("/games/games/evaluate", response_model=EvaluateGameResponse)
    async def evaluate_game(request: EvaluateGameRequest) -> EvaluateGameResponse:
        """Evaluate game state."""
        try:
            status = game_service.get_game_status(request.game_type, request.game_state)
            winner_id = game_service.get_winner_id(request.game_type, request.game_state)
            current_player_id = game_service.get_current_player_id(request.game_type, request.game_state)
            is_finished = status not in ["ongoing"]
            
            return EvaluateGameResponse(
                status=status,
                winner_id=winner_id,
                is_finished=is_finished,
                current_player_id=current_player_id
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to evaluate game: {str(e)}")
    
    @router.get("/games/sessions/player/{player_id}/history")
    async def get_match_history(player_id: str, limit: int = 20) -> List[Dict[str, Any]]:
        """Get match history for a player.
        
        Returns finished game sessions for the specified player, ordered by most recent first.
        """
        sessions = session_service.get_match_history(player_id=player_id, limit=limit)
        
        # Convert to response format
        history = []
        for session in sessions:
            history.append({
                "session_id": session.session_id,
                "game_id": session.game_id,
                "game_type": session.game_type,
                "status": session.status.value,
                "player_ids": session.player_ids,
                "winner_id": session.winner_id,
                "total_moves": session.total_moves,
                "started_at": session.started_at.isoformat() if session.started_at else None,
                "ended_at": session.ended_at.isoformat() if session.ended_at else None,
            })
        
        return history
    
    @router.post("/games/sessions/{session_id}/abandon", response_model=AbandonSessionResponse)
    async def abandon_session(
        session_id: str,
        request: AbandonSessionRequest = Body(...)
    ) -> AbandonSessionResponse:
        """Abandon a game session. Allows force abandoning even if session is not active."""
        try:
            # First try normal abandon
            try:
                session = session_service.abandon_session(
                    session_id=session_id,
                    player_id=request.player_id,
                    force=False
                )
            except ValueError as e:
                # If session is not active, try force abandon
                if "not active" in str(e).lower():
                    session = session_service.abandon_session(
                        session_id=session_id,
                        player_id=request.player_id,
                        force=True
                    )
                else:
                    raise
            
            return AbandonSessionResponse(
                session_id=session.session_id,
                status=session.status.value,
                winner_id=session.winner_id,
                message=f"Session abandoned by player {request.player_id}"
            )
        except ValueError as e:
            raise ValidationError(str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to abandon session: {str(e)}")
    
    return router
