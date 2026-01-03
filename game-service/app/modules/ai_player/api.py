"""
AI Player API
REST endpoints for AI player.
"""
import time
import uuid
import random
from fastapi import APIRouter, HTTPException, BackgroundTasks

from .service import AIPlayerService
from .models import AIPlayerLevel
from .dto import (
    MoveRequest, MoveResponse, DifficultyRequest, DifficultyResponse, 
    LevelsResponse, LevelInfo, SelfPlayRequest, SelfPlayResponse, GameResult
)


def create_router(service: AIPlayerService) -> APIRouter:
    """Create API router."""
    router = APIRouter(tags=["AI Player"])
    
    @router.post("/move", response_model=MoveResponse)
    async def get_ai_move(request: MoveRequest):
        """
        Get AI's best move.
        
        Send the current board state and difficulty level,
        AI returns which column to play.
        """
        try:
            # Extract player_id from game_state
            player_id = request.game_state.get("current_player", "ai_p2")
            
            result = service.get_move(
                game_type=request.game_type,
                game_state=request.game_state,
                player_id=player_id,
                level=request.level,
            )
            return MoveResponse(
                move=result.move,
                level=request.level,
                confidence=result.confidence,
                thinking_time_ms=result.time_taken * 1000,
                iterations=result.iterations,
            )
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"AI error: {str(e)}")
    
    @router.post("/adjust-difficulty", response_model=DifficultyResponse)
    async def adjust_difficulty(request: DifficultyRequest):
        """
        Get recommended difficulty based on player performance.
        
        If player wins too much -> increase difficulty
        If player loses too much -> decrease difficulty
        """
        # Convert string level to enum
        level_enum = AIPlayerLevel(request.current_level)
        
        result = service.adjust_difficulty(
            current_level=level_enum,
            win_rate=request.player_win_rate,
        )
        return DifficultyResponse(
            recommended_level=result.recommended_level.value,
            reason=result.reason,
        )
    
    @router.get("/levels", response_model=LevelsResponse)
    async def get_levels():
        """Get available difficulty levels."""
        return LevelsResponse(
            levels=[
                LevelInfo(level=level.value, iterations=service.level_iterations[level])
                for level in AIPlayerLevel
            ]
        )
    
    @router.post("/self-play", response_model=SelfPlayResponse)
    async def run_self_play(request: SelfPlayRequest):
        """
        Run AI vs AI games for training data generation.
        
        Multiple concurrent agents play against each other,
        generating gameplay data automatically.
        
        - **num_games**: Number of games to play (1-1000)
        - **player1_level**: AI difficulty for player 1
        - **player2_level**: AI difficulty for player 2
        - **epsilon**: Random move probability (0.0-1.0) for variety
        - **log_to_db**: Whether to log games for ML training
        """
        try:
            from app.modules.game_engine.service import GameEngineService
            from app.modules.game_engine.models import Player, GameStatus
            from app.modules.game_engine.utils.serialization import state_to_dict
            from app.modules.ai_player.mcts import MCTSAlgorithm
            
            # Optional: game logger
            logger = None
            if request.log_to_db:
                try:
                    from app.modules.game_logger.service import GameLoggerService
                    from app.shared.container import container
                    if container.has(GameLoggerService):
                        logger = container.get(GameLoggerService)
                    else:
                        logger = GameLoggerService()
                except Exception:
                    pass  # Continue without logging
            
            engine = GameEngineService()
            mcts = MCTSAlgorithm(engine)
            
            # Get iterations for each level
            p1_iterations = service.iterations[request.player1_level]
            p2_iterations = service.iterations[request.player2_level]
            
            # Stats
            results = {"win_p1": 0, "win_p2": 0, "draw": 0}
            total_moves = 0
            games = []
            start_time = time.time()
            
            for _ in range(request.num_games):
                game_id = str(uuid.uuid4())
                state = engine.new_game()
                move_count = 0
                game_start = time.time()
                
                # Create session if logging
                if logger:
                    try:
                        logger.create_game_session(
                            game_id=game_id,
                            game_type="connect_four",
                            player1_id="ai_p1",
                            player2_id="ai_p2",
                            player1_type=f"mcts_{p1_iterations}",
                            player2_type=f"mcts_{p2_iterations}",
                        )
                    except Exception:
                        pass
                
                # Play game
                while state.status == GameStatus.ONGOING:
                    iterations = p1_iterations if state.current_player == Player.P1 else p2_iterations
                    result = mcts.search(state, iterations=iterations)
                    
                    legal_moves = engine.get_legal_moves(state)
                    if random.random() < request.epsilon:
                        move = random.choice(legal_moves)
                    else:
                        move = result.best_move
                    
                    state_before = state
                    state = engine.apply_move(state, move)
                    move_count += 1
                    
                    # Log move
                    if logger:
                        try:
                            logger.log_move(
                                game_id=game_id,
                                game_type="connect_four",
                                move_index=move_count,
                                player_id=f"ai_p{1 if state_before.current_player == Player.P1 else 2}",
                                agent_type=f"mcts_{iterations}",
                                state_before=state_to_dict(state_before),
                                move_data={"column": move},
                                state_after=state_to_dict(state),
                                result=state.status.value,
                                heuristic_value=result.win_rate,
                                mcts_visit_count=result.visits,
                            )
                        except Exception:
                            pass
                
                game_duration = time.time() - game_start
                
                # Determine winner
                winner = state.status.value
                if winner == "win_p1":
                    results["win_p1"] += 1
                elif winner == "win_p2":
                    results["win_p2"] += 1
                else:
                    results["draw"] += 1
                
                total_moves += move_count
                games.append(GameResult(
                    game_id=game_id,
                    winner=winner,
                    total_moves=move_count,
                    duration_seconds=round(game_duration, 3)
                ))
            
            total_duration = time.time() - start_time
            num_games = request.num_games
            
            return SelfPlayResponse(
                total_games=num_games,
                player1_wins=results["win_p1"],
                player2_wins=results["win_p2"],
                draws=results["draw"],
                player1_win_rate=round(results["win_p1"] / num_games, 3),
                player2_win_rate=round(results["win_p2"] / num_games, 3),
                avg_moves_per_game=round(total_moves / num_games, 1),
                total_duration_seconds=round(total_duration, 2),
                games=games if num_games <= 100 else None  # Only return details for small batches
            )
            
        except ImportError as e:
            raise HTTPException(status_code=500, detail=f"Module not available: {str(e)}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Self-play error: {str(e)}")
    
    return router
