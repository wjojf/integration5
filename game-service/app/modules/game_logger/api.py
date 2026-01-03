"""
Game Logger API - ML-Focused
FastAPI router for ML training data logging endpoints.
"""
from fastapi import APIRouter, HTTPException
from typing import Optional, Dict

from app.shared.exceptions import ServiceError
from app.config import settings
from .service import GameLoggerService
from .dataset_export import DatasetExportService
from .dvc_manager import DVCManager
from .dto import (
    CreateSessionRequest,
    CreateSessionResponse,
    FinishSessionRequest,
    FinishSessionResponse,
    LogMoveRequest,
    LogMoveResponse,
    MoveListResponse,
    MoveItem,
    SessionStatsResponse,
    ExportDatasetRequest,
    ExportDatasetResponse,
    DatasetStatsResponse,
    DatasetVersionStats,
    DVCInitRequest,
    DVCInitResponse,
)


def create_router(service: GameLoggerService) -> APIRouter:
    """Create router with injected service dependency."""
    router = APIRouter(tags=["Game Logger"])


    # ==================== SESSION ENDPOINTS ====================

    @router.post("/session/create", response_model=CreateSessionResponse)
    async def create_session(request: CreateSessionRequest):
        """
        Create a new game session for ML logging.

        Records session metadata including player configurations,
        agent types, and experiment tags for dataset organization.
        """
        try:
            result = service.create_session(
                session_id=request.session_id,
                game_type=request.game_type,
                p1_type=request.p1_type,
                p2_type=request.p2_type,
                p1_agent=request.p1_agent,
                p2_agent=request.p2_agent,
                p1_agent_level=request.p1_agent_level,
                p2_agent_level=request.p2_agent_level,
                tag=request.tag,
            )

            if result is None:
                raise ServiceError("Failed to create session")

            return CreateSessionResponse(
                status="created",
                session_id=result["session_id"],
                message="Session created successfully"
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to create session: {str(e)}")

    @router.post("/session/finish", response_model=FinishSessionResponse)
    async def finish_session(request: FinishSessionRequest):
        """
        Mark a game session as finished with final results.

        Records winner, total moves, and match duration for
        dataset statistics and analysis.
        """
        try:
            result = service.finish_session(
                session_id=request.session_id,
                winner=request.winner,
                num_moves=request.num_moves,
                duration_seconds=request.duration_seconds,
                end_time=request.end_time,
            )

            if result is None:
                raise ServiceError("Failed to finish session")

            return FinishSessionResponse(
                status="finished",
                session_id=result["session_id"],
                winner=result["winner"],
                num_moves=result["num_moves"],
                message="Session finished successfully"
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to finish session: {str(e)}")

    @router.get("/session/{session_id}/stats", response_model=SessionStatsResponse)
    async def get_session_stats(session_id: str):
        """Get statistics for a specific session."""
        try:
            result = service.get_session(session_id)

            if result is None:
                raise HTTPException(status_code=404, detail=f"Session {session_id} not found")

            return SessionStatsResponse(**result)
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get session stats: {str(e)}")

    @router.get("/sessions/tag/{tag}")
    async def get_sessions_by_tag(tag: str, limit: int = 100):
        """Get sessions filtered by experiment tag."""
        try:
            sessions = service.get_sessions_by_tag(tag, limit)
            return {
                "tag": tag,
                "total": len(sessions),
                "sessions": sessions
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get sessions by tag: {str(e)}")

    # ==================== MOVE ENDPOINTS ====================

    @router.post("/move/log", response_model=LogMoveResponse)
    async def log_move(request: LogMoveRequest):
        """
        Log a single move with ML training data.

        Records board state, legal moves, played move, and expert labels
        (MCTS policy distribution and value) for supervised learning.

        Key ML fields:
        - board_flat: [42] flattened board for neural network input
        - expert_policy: [7] MCTS probability distribution (soft label)
        - expert_best_move: argmax(expert_policy) (hard label)
        - expert_value: Win probability [0-1] from MCTS
        """
        try:
            result = service.log_move(
                session_id=request.session_id,
                move_number=request.move_number,
                current_player=request.current_player,
                current_player_id=request.current_player_id,
                board_state=request.board_state,
                board_flat=request.board_flat,
                legal_moves_mask=request.legal_moves_mask,
                played_move=request.played_move,
                expert_policy=request.expert_policy,
                expert_best_move=request.expert_best_move,
                expert_value=request.expert_value,
                final_result=request.final_result,
                final_result_numeric=request.final_result_numeric,
                mcts_iterations=request.mcts_iterations,
                game_status=request.game_status,
            )

            if result is None:
                raise ServiceError("Failed to log move")

            return LogMoveResponse(
                status="logged",
                move_id=result["id"],
                session_id=result["session_id"],
                move_number=result["move_number"],
                message="Move logged successfully"
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to log move: {str(e)}")

    @router.get("/session/{session_id}/moves", response_model=MoveListResponse)
    async def get_session_moves(session_id: str):
        """
        Retrieve all moves for a session, ordered by move_number.

        Returns complete move history with expert labels for analysis
        and ML training data verification.
        """
        try:
            moves = service.get_session_moves(session_id)

            return MoveListResponse(
                session_id=session_id,
                total_moves=len(moves),
                moves=[MoveItem(**move) for move in moves]
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get session moves: {str(e)}")

    @router.get("/moves/recent")
    async def get_recent_moves(limit: int = 100):
        """Get recent moves across all sessions."""
        try:
            moves = service.get_recent_moves(limit)
            return {
                "total": len(moves),
                "moves": moves
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get recent moves: {str(e)}")

    @router.get("/move/{move_id}")
    async def get_move(move_id: int):
        """Get a specific move by ID."""
        try:
            move = service.get_move(move_id)

            if move is None:
                raise HTTPException(status_code=404, detail=f"Move {move_id} not found")

            return move
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get move: {str(e)}")

    # ==================== STATISTICS ====================

    @router.get("/stats")
    async def get_stats():
        """
        Get overall database statistics.

        Returns counts of sessions, moves, and performance metrics
        for monitoring data collection progress.
        """
        try:
            return service.get_stats()
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get stats: {str(e)}")

    # ==================== DATASET EXPORT (OPTIONAL) ====================

    if not getattr(settings, "DATASET_EXPORT_ENABLED", False):
        return router

    export_service = DatasetExportService(service)
    dvc_manager = DVCManager()

    @router.post("/export-dataset", response_model=ExportDatasetResponse)
    async def export_dataset(request: ExportDatasetRequest):
        """
        Export game moves to Parquet file for ML training.

        Filters data by experiment tag, agent types, and minimum game count.
        Outputs compressed Parquet file optimized for PyTorch/TensorFlow dataloaders.
        """
        try:
            result = export_service.export_to_parquet(
                version=request.version,
                limit=request.limit,
                agent_types=request.agent_types,
                min_games=request.min_games,
                tag=request.tag,
            )

            _handle_dvc_tracking(dvc_manager, result, request.version)

            return ExportDatasetResponse(
                status="exported",
                version=result.version,
                file_path=str(result.file_path),
                file_size_mb=result.file_size_mb,
                total_records=result.total_records,
                message="Dataset exported successfully"
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to export dataset: {str(e)}")

    @router.get("/dataset-stats", response_model=DatasetStatsResponse)
    async def get_dataset_stats(version: Optional[str] = None):
        """
        Get statistics about exported datasets.

        Returns information about dataset versions, record counts,
        agent distributions, and date ranges.
        """
        try:
            stats_dict = export_service.get_dataset_stats(version=version)
            stats_dto = _convert_stats_to_dto(stats_dict)
            return DatasetStatsResponse(stats=stats_dto)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to get dataset stats: {str(e)}")

    @router.post("/dvc/init", response_model=DVCInitResponse)
    async def init_dvc(request: DVCInitRequest):
        """
        Initialize DVC repository for dataset versioning.

        Sets up DVC with specified remote storage (local, S3, MinIO)
        for tracking and sharing ML training datasets.
        """
        try:
            result = dvc_manager.initialize(
                remote_type=request.remote_type,
                remote_url=request.remote_url or getattr(settings, "DVC_REMOTE_URL", None),
            )
            return DVCInitResponse(
                status="initialized" if result.initialized else "failed",
                message=result.message,
            )
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to initialize DVC: {str(e)}")

    @router.post("/dvc/push/{version}")
    async def push_dataset_to_dvc(version: str):
        """
        Push dataset version to DVC remote storage.

        Uploads dataset to configured remote (S3, MinIO, etc.) and
        creates .dvc tracking file for version control.
        """
        try:
            stats_dict = export_service.get_dataset_stats(version=version)

            if version not in stats_dict:
                raise HTTPException(status_code=404, detail=f"Dataset version {version} not found")

            dataset_path = stats_dict[version].file
            dvc_manager.push_dataset(dataset_path)

            return {"status": "pushed", "version": version, "message": "Dataset pushed to DVC remote"}
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to push dataset: {str(e)}")

    return router


# ==================== HELPER FUNCTIONS ====================

def _handle_dvc_tracking(dvc_manager: DVCManager, result, version: str) -> Optional[str]:
    """Handle DVC tracking for exported dataset."""
    if not getattr(settings, "DVC_ENABLED", False):
        return None

    if not dvc_manager.is_initialized():
        dvc_manager.initialize(
            remote_type=getattr(settings, "DVC_REMOTE_TYPE", "local"),
            remote_url=getattr(settings, "DVC_REMOTE_URL", None),
        )

    return dvc_manager.add_dataset(str(result.file_path), version)


def _convert_stats_to_dto(stats_dict: Dict) -> Dict[str, DatasetVersionStats]:
    """Convert DatasetStats to DTO format."""
    return {
        version: DatasetVersionStats(
            version=stat.version,
            file=stat.file,
            total_records=stat.total_records,
            unique_sessions=stat.unique_sessions,
            agent_distribution=stat.agent_distribution,
            date_range=stat.date_range,
        )
        for version, stat in stats_dict.items()
    }
