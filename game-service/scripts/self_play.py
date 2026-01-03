"""
Self-Play Script - ML Dataset Generation
Runs AI vs AI games to collect training data with expert labels.

Usage:
    python -m scripts.self_play --games 100
    python -m scripts.self_play --games 1000 --epsilon 0.1 --tag selfplay_v1
"""
import argparse
import random
import time
import uuid
from typing import Dict, Any, Optional, List, TYPE_CHECKING

# Direct imports to avoid loading entire games module
from app.modules.games.games.connect_four.game import ConnectFourGame
from app.modules.games.services.game_service import GameService
from app.core.game.factory import GameFactory
from app.core.game.registry import GameRegistry
from app.modules.ai_player.mcts import MCTSAlgorithm

if TYPE_CHECKING:
    from app.modules.game_logger.service import GameLoggerService


# Skill levels: name -> MCTS iterations
SKILL_LEVELS = {
    "weak": 50,
    "low": 100,
    "medium": 500,
    "high": 1000,
    "strong": 2000,
}

# Skill level mapping
SKILL_TO_LEVEL = {
    "weak": "low",
    "low": "low",
    "medium": "medium",
    "high": "high",
    "strong": "high",
}

# Game status constants
STATUS_ONGOING = "ongoing"
STATUS_WIN_P1 = "win_p1"
STATUS_WIN_P2 = "win_p2"
STATUS_DRAW = "draw"


def flatten_board(board: List[List[int]]) -> List[int]:
    """
    Flatten Connect Four board (6x7) to 1D array (42 elements).

    Board format: 0 = empty, 1 = player 1, 2 = player 2
    """
    return [cell for row in board for cell in row]


def get_legal_moves_mask(legal_moves: List[Dict]) -> List[int]:
    """Convert legal moves to binary mask [7 elements]."""
    mask = [0] * 7
    for move in legal_moves:
        col = move.get("column", -1)
        if 0 <= col <= 6:
            mask[col] = 1
    return mask


def mcts_policy_to_array(mcts_result, legal_moves: List[Dict]) -> List[float]:
    """
    Convert MCTS visit counts to probability distribution [7 elements].

    Args:
        mcts_result: MCTS search result with move_visits dict
        legal_moves: List of legal moves

    Returns:
        Probability distribution over 7 columns
    """
    # Initialize with zeros
    policy = [0.0] * 7

    # Get visit counts from MCTS result
    # FIXED: Use move_visits (dict[int, int]) instead of move_stats
    if hasattr(mcts_result, 'move_visits') and mcts_result.move_visits:
        # move_visits is dict: {column_index: visit_count}
        total_visits = sum(mcts_result.move_visits.values())

        if total_visits > 0:
            for move_idx, visits in mcts_result.move_visits.items():
                if 0 <= move_idx <= 6:
                    policy[move_idx] = visits / total_visits
    else:
        # Fallback: uniform distribution over legal moves
        legal_cols = [m.get("column", -1) for m in legal_moves if 0 <= m.get("column", -1) <= 6]
        if legal_cols:
            prob = 1.0 / len(legal_cols)
            for col in legal_cols:
                policy[col] = prob

    # Normalize to ensure sum = 1.0
    total = sum(policy)
    if total > 0:
        policy = [p / total for p in policy]

    return policy


def play_game(
    game_service: GameService,
    mcts: MCTSAlgorithm,
    p1_iterations: int,
    p2_iterations: int,
    p1_skill: str,
    p2_skill: str,
    epsilon: float = 0.1,
    tag: Optional[str] = None,
    logger: Optional["GameLoggerService"] = None
) -> Dict[str, Any]:
    """
    Play one game between two AI agents with ML-focused logging.

    Args:
        game_service: Game service instance
        mcts: MCTS algorithm instance
        p1_iterations: MCTS iterations for player 1
        p2_iterations: MCTS iterations for player 2
        p1_skill: Skill level name for player 1
        p2_skill: Skill level name for player 2
        epsilon: Random move probability
        tag: Experiment tag for dataset organization
        logger: Game logger service (optional)

    Returns:
        Game result dictionary
    """
    session_id = str(uuid.uuid4())
    game_type = "connect_four"
    player1_id = "ai_p1"
    player2_id = "ai_p2"

    # Create initial state
    state = game_service.create_initial_state(
        game_type=game_type,
        player_ids=[player1_id, player2_id],
        starting_player_id=player1_id
    )

    move_count = 0
    start_time = time.time()

    # Create game session in DB (NEW SCHEMA)
    if logger:
        logger.create_session(
            session_id=session_id,
            game_type=game_type,
            p1_type="ai",
            p2_type="ai",
            p1_agent=f"mcts_{p1_iterations}",
            p2_agent=f"mcts_{p2_iterations}",
            p1_agent_level=SKILL_TO_LEVEL[p1_skill],
            p2_agent_level=SKILL_TO_LEVEL[p2_skill],
            tag=tag,
        )

    # Play until game ends
    while game_service.get_game_status(game_type, state) == STATUS_ONGOING:
        current_player = game_service.get_current_player_id(game_type, state)
        iterations = p1_iterations if current_player == player1_id else p2_iterations

        # Get legal moves
        legal_moves = game_service.get_legal_moves(game_type, state, current_player)

        if not legal_moves:
            break

        # Run MCTS search to get expert labels
        mcts_result = mcts.search(state, current_player, iterations=iterations)

        # Epsilon-greedy: sometimes pick random move for exploration
        if random.random() < epsilon:
            move = random.choice(legal_moves)
        else:
            move = mcts_result.best_move

        # Extract board state (assuming state has 'board' key)
        board = state.get("board", [])

        # Use raw board without normalization (0 = empty, 1 = p1, 2 = p2)
        board_flat = flatten_board(board)

        # Get legal moves mask
        legal_mask = get_legal_moves_mask(legal_moves)

        # Get expert policy (MCTS visit distribution)
        expert_policy = mcts_policy_to_array(mcts_result, legal_moves)
        expert_best_move = mcts_result.best_move.get("column", 0)
        expert_value = mcts_result.win_rate  # Win probability from MCTS

        # Apply move
        state_after = game_service.apply_move(game_type, state, move, current_player)
        move_count += 1

        # Determine game result from current player's perspective
        game_status_raw = game_service.get_game_status(game_type, state_after)
        winner_id = game_service.get_winner_id(game_type, state_after)

        # FIXED: Use winner_id directly for clearer logic
        if game_status_raw == STATUS_ONGOING:
            final_result = "ongoing"
            final_result_numeric = None
        elif game_status_raw == STATUS_DRAW:
            final_result = "draw"
            final_result_numeric = 0.5
        elif winner_id == current_player:
            # Current player won
            final_result = "win"
            final_result_numeric = 1.0
        elif winner_id is not None:
            # Current player lost
            final_result = "loss"
            final_result_numeric = 0.0
        else:
            # Ongoing or unknown state
            final_result = "ongoing"
            final_result_numeric = None

        # Log move to DB (NEW SCHEMA)
        if logger:
            # Determine player ID (1 or 2) based on player position
            player_id_num = 1 if current_player == player1_id else 2

            logger.log_move(
                session_id=session_id,
                move_number=move_count,
                current_player="p1" if current_player == player1_id else "p2",
                current_player_id=player_id_num,  # 1 for p1, 2 for p2
                board_state={"board": board, "current_player": player_id_num},
                board_flat=board_flat,
                legal_moves_mask=legal_mask,
                played_move=move.get("column", 0),
                expert_policy=expert_policy,
                expert_best_move=expert_best_move,
                expert_value=expert_value,
                final_result=final_result,
                final_result_numeric=final_result_numeric,
                mcts_iterations=iterations,
                game_status="ongoing" if game_status_raw == STATUS_ONGOING else "finished",
            )

        # Update state for next iteration
        state = state_after

    duration = time.time() - start_time
    final_status = game_service.get_game_status(game_type, state)

    # Convert final status to winner format
    if final_status == STATUS_WIN_P1:
        winner = "p1"
    elif final_status == STATUS_WIN_P2:
        winner = "p2"
    else:
        winner = "draw"

    # Finish game session (NEW SCHEMA)
    if logger:
        logger.finish_session(
            session_id=session_id,
            winner=winner,
            num_moves=move_count,
            duration_seconds=duration,
        )

    return {
        "session_id": session_id,
        "winner": final_status,
        "moves": move_count,
        "duration": duration,
    }


def run_self_play(num_games: int, epsilon: float = 0.1, tag: Optional[str] = None, use_db: bool = True):
    """
    Run multiple self-play games for ML dataset generation.

    Args:
        num_games: Number of games to play
        epsilon: Random move probability for exploration
        tag: Experiment tag (e.g., 'selfplay_v1')
        use_db: Whether to log to database
    """
    # Initialize services
    registry = GameRegistry()
    registry.register(ConnectFourGame)
    game_factory = GameFactory(registry)
    game_service = GameService(game_factory)

    # Use MCTS from ai_player module
    mcts = MCTSAlgorithm(game_service, game_type="connect_four")

    # Only import logger if using DB
    logger = None
    if use_db:
        from app.modules.game_logger.service import GameLoggerService
        logger = GameLoggerService()

    # Different matchups for variety
    matchups = [
        ("low", "low"),
        ("low", "medium"),
        ("medium", "medium"),
        ("medium", "high"),
        ("high", "high"),
        ("weak", "strong"),
    ]

    # Stats
    results = {"win_p1": 0, "win_p2": 0, "draw": 0}
    total_moves = 0

    print(f"Starting self-play: {num_games} games, epsilon={epsilon}, tag={tag}")
    print("-" * 60)

    for i in range(num_games):
        # Pick random matchup
        p1_skill, p2_skill = random.choice(matchups)
        p1_iter = SKILL_LEVELS[p1_skill]
        p2_iter = SKILL_LEVELS[p2_skill]

        # Play game
        try:
            result = play_game(
                game_service, mcts, p1_iter, p2_iter, p1_skill, p2_skill, epsilon, tag, logger
            )

            # Update stats
            total_moves += result["moves"]
            if result["winner"] == STATUS_WIN_P1:
                results["win_p1"] += 1
            elif result["winner"] == STATUS_WIN_P2:
                results["win_p2"] += 1
            else:
                results["draw"] += 1

            # Progress
            if (i + 1) % 10 == 0:
                print(f"[{i + 1:4d}/{num_games}] {p1_skill:8s} vs {p2_skill:8s} | "
                      f"Moves: {result['moves']:2d} | Winner: {result['winner']}")

        except Exception as e:
            print(f"[ERROR] Game {i + 1} failed: {e}")
            import traceback
            traceback.print_exc()
            continue

    # Final stats
    print("-" * 60)
    print(f"✓ Completed: {num_games} games played")
    if num_games > 0:
        print(f"  P1 wins: {results['win_p1']:4d} ({100*results['win_p1']/num_games:5.1f}%)")
        print(f"  P2 wins: {results['win_p2']:4d} ({100*results['win_p2']/num_games:5.1f}%)")
        print(f"  Draws:   {results['draw']:4d} ({100*results['draw']/num_games:5.1f}%)")
        print(f"  Avg moves per game: {total_moves/num_games:.1f}")

    if use_db and tag:
        print(f"\n✓ Dataset logged to database with tag: '{tag}'")
        print(f"  Export with: POST /api/v1/game-logger/export-dataset {{\"version\": \"v1\", \"tag\": \"{tag}\"}}")


def main():
    parser = argparse.ArgumentParser(description="Self-play for ML dataset generation")
    parser.add_argument("--games", "-g", type=int, default=100, help="Number of games to play")
    parser.add_argument("--epsilon", "-e", type=float, default=0.1, help="Random move probability (0-1)")
    parser.add_argument("--tag", "-t", type=str, default="selfplay", help="Dataset tag (e.g., 'selfplay_v1')")
    parser.add_argument("--no-db", action="store_true", help="Disable database logging")

    args = parser.parse_args()
    run_self_play(args.games, args.epsilon, args.tag, use_db=not args.no_db)


if __name__ == "__main__":
    main()
