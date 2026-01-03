# app/modules/game_logger/monitoring.py

"""
Model Monitoring Service

Logs model / agent quality metrics to Weights & Biases (or another backend).
"""

from dataclasses import dataclass
from typing import Optional, Dict, Any, List
from app.config import settings
from .service import GameLoggerService
from .repositories.models import GameMoveModel, SessionLogModel
import os

try:
    import wandb
except ImportError:  # soft dependency
    wandb = None




@dataclass
class MonitoringConfig:
    project: str = "banditgames-monitoring"
    entity: Optional[str] = None
    enabled: bool = True
    run_name: Optional[str] = None


class ModelMonitoringService:
    """
    High-level interface to log metrics to a monitoring backend (W&B).

    Usage:
        monitor = ModelMonitoringService()
        monitor.log_game_summary(...)
        monitor.log_ai_move(...)
    """

    def __init__(self, config: Optional[MonitoringConfig] = None) -> None:
        self.config = config or MonitoringConfig()

        # Allow disabling via settings
        self.enabled = bool(
            getattr(settings, "MONITORING_ENABLED", True) and self.config.enabled
        )

        self._run = None
        if self.enabled and wandb is not None:
            self._run = wandb.init(
                project=self.config.project,
                entity=self.config.entity,
                name=self.config.run_name,
                config={
                    "env": getattr(settings, "ENVIRONMENT", "local"),
                },
            )

    # ------------- low-level logging helpers -------------

    def _log(self, metrics: Dict[str, Any], step: Optional[int] = None) -> None:
        if not self.enabled or self._run is None:
            return
        if step is not None:
            metrics["step"] = step
        wandb.log(metrics)

    def finish(self) -> None:
        if self._run is not None:
            self._run.finish()
            self._run = None

    # ------------- public API: per-move logging -------------

    def log_ai_move(
        self,
        *,
        game_id: str,
        move_index: int,
        agent_type: str,
        move_column: int,
        best_move_column: Optional[int] = None,
        move_quality_score: Optional[float] = None,
        thinking_time_ms: Optional[float] = None,
        is_ml_model: bool = False,
        is_human: bool = False,
    ) -> None:
        """
        Log a single move made by any kind of player.

        This gives you "visual comparison of moves" + "speed comparison".
        """
        correct_move = (
            best_move_column is not None and best_move_column == move_column
        )

        self._log(
            {
                "game_id": game_id,
                "move_index": move_index,
                "agent_type": agent_type,
                "is_ml_model": int(is_ml_model),
                "is_human": int(is_human),
                "move_column": move_column,
                "best_move_column": best_move_column,
                "move_quality_score": move_quality_score,
                "correct_move": int(correct_move),
                "thinking_time_ms": thinking_time_ms,
            },
            step=move_index,
        )

    # ------------- public API: per-game summary -------------

    def log_game_summary(
        self,
        *,
        game_id: str,
        player1_type: str,
        player2_type: str,
        winner: Optional[str],
        total_moves: int,
        duration_seconds: Optional[float],
    ) -> None:
        """
        Log aggregate data per finished game.

        Use this for win-rate / duration comparisons between agent types.
        """
        winner_label = winner or "draw"

        self._log(
            {
                "game_id": game_id,
                "metric/type_player1": player1_type,
                "metric/type_player2": player2_type,
                "metric/winner": winner_label,
                "metric/total_moves": total_moves,
                "metric/duration_seconds": duration_seconds,
            }
        )

    # ------------- convenience: derive metrics from DB -------------

    def log_from_database(self, logger_service: GameLoggerService) -> None:
        """
        Offline monitoring: iterate over existing sessions and logs
        and push aggregate metrics to W&B.
        """
        repository = logger_service.repository
        repository.initialize()

        with repository.get_session() as session:
            sessions = session.query(SessionLogModel).all()

            for sess in sessions:
                game_id = sess.session_id

                player1_label = sess.p1_agent or sess.p1_type
                player2_label = sess.p2_agent or sess.p2_type

                # per-game summary
                self.log_game_summary(
                    game_id=game_id,
                    player1_type=player1_label,
                    player2_type=player2_label,
                    winner=sess.winner,
                    total_moves=sess.num_moves,
                    duration_seconds=sess.duration_seconds,
                )

                logs = (
                    session.query(GameMoveModel)
                    .filter(GameMoveModel.session_id == sess.session_id)
                    .order_by(GameMoveModel.move_number)
                    .all()
                )

                for log in logs:
                    quality = None
                    if isinstance(log.expert_policy, list) and 0 <= log.played_move < len(log.expert_policy):
                        quality = float(log.expert_policy[log.played_move])

                    if log.current_player_id == 1:
                        agent_type = sess.p1_agent or sess.p1_type
                        is_human = (sess.p1_type == "human")
                    else:
                        agent_type = sess.p2_agent or sess.p2_type
                        is_human = (sess.p2_type == "human")

                    is_ml_model = "ml" in (agent_type or "").lower()

                    self.log_ai_move(
                        game_id=game_id,
                        move_index=log.move_number,
                        agent_type=agent_type or "unknown",
                        move_column=log.played_move,
                        best_move_column=log.expert_best_move,
                        move_quality_score=quality,
                        thinking_time_ms=None,
                        is_ml_model=is_ml_model,
                        is_human=is_human,
                    )


