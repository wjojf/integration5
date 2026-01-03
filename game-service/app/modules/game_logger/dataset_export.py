"""
Dataset Export Service
Handles exporting game logs to parquet files for ML training with DVC versioning.
"""
from pathlib import Path
from typing import Optional, List, Dict
from datetime import datetime
import pandas as pd
import shutil
from .dvc_manager import DVCManager
from app.config import settings
from .repositories.models import SessionLogModel, GameMoveModel
from .service import GameLoggerService
from .types import GameLogRow, DatasetStats, DatasetExportResult


class DatasetExportService:
    """Service for exporting game logs to datasets with DVC versioning."""
    
    def __init__(self, logger_service: GameLoggerService):
        """Initialize the dataset export service."""
        self.logger_service = logger_service
        self.export_path = Path(getattr(settings, "DATASET_EXPORT_PATH", "./data/datasets"))
        self.export_path.mkdir(parents=True, exist_ok=True)
    
    def _build_game_log_row(self, move: GameMoveModel, session: Optional[SessionLogModel]) -> GameLogRow:
        """Build a structured game log row from database models (NEW SCHEMA)."""
        return GameLogRow(
            id=move.id,
            session_id=move.session_id,
            move_number=move.move_number,
            current_player=move.current_player,
            current_player_id=move.current_player_id,
            board_state=move.board_state,
            board_flat=list(move.board_flat) if move.board_flat else [],
            legal_moves_mask=list(move.legal_moves_mask) if move.legal_moves_mask else [],
            played_move=move.played_move,
            expert_policy=list(move.expert_policy) if move.expert_policy else [],
            expert_best_move=move.expert_best_move,
            expert_value=move.expert_value,
            final_result=move.final_result,
            final_result_numeric=move.final_result_numeric,
            mcts_iterations=move.mcts_iterations,
            game_status=move.game_status,
            timestamp=move.timestamp.isoformat() if move.timestamp else None,
            game_type=session.game_type if session else None,
            p1_type=session.p1_type if session else None,
            p2_type=session.p2_type if session else None,
            p1_agent=session.p1_agent if session else None,
            p2_agent=session.p2_agent if session else None,
            p1_agent_level=session.p1_agent_level if session else None,
            p2_agent_level=session.p2_agent_level if session else None,
            winner=session.winner if session else None,
            num_moves=session.num_moves if session else None,
            duration_seconds=session.duration_seconds if session else None,
            tag=session.tag if session else None,
        )
    
    def _get_session_info(self, repository, game_id: str) -> Optional[SessionLogModel]:
        """Get session information for a game."""
        with repository.get_session() as db_session:
            return db_session.query(SessionLogModel).filter(
                SessionLogModel.session_id == game_id
            ).first()
    
    def _filter_by_min_games(self, df: pd.DataFrame, min_games: int) -> pd.DataFrame:
        """Filter dataframe by minimum games per agent type."""
        if df.empty:
            return df

        # Count games per agent across p1_agent and p2_agent columns
        game_counts = {}
        for agent_col in ['p1_agent', 'p2_agent']:
            if agent_col in df.columns:
                counts = df.groupby(agent_col)['session_id'].nunique()
                for agent, count in counts.items():
                    if agent:  # Skip None/null agents
                        game_counts[agent] = game_counts.get(agent, 0) + count

        valid_agents = {agent for agent, count in game_counts.items() if count >= min_games}
        if not valid_agents:
            return pd.DataFrame()

        # Keep rows where either p1_agent or p2_agent is a valid agent
        mask = (df['p1_agent'].isin(valid_agents)) | (df['p2_agent'].isin(valid_agents))
        return df[mask]
    
    def export_game_logs_to_dataframe(
        self,
        limit: Optional[int] = None,
        agent_types: Optional[List[str]] = None,
        min_games: Optional[int] = None,
        tag: Optional[str] = None
    ) -> pd.DataFrame:
        """
        Export game logs from database to pandas DataFrame.
        
        Args:
            limit: Maximum number of logs to export (None for all)
            agent_types: Filter by agent types (None for all)
            min_games: Minimum number of games per agent type
            
        Returns:
            DataFrame with game log data
        """
        repository = self.logger_service.repository
        
        with repository.get_session() as db_session:
            query = db_session.query(GameMoveModel)

            if tag or agent_types:
                query = query.join(
                    SessionLogModel,
                    SessionLogModel.session_id == GameMoveModel.session_id
                )

            if tag:
                query = query.filter(SessionLogModel.tag == tag)

            if agent_types:
                query = query.filter(
                    (SessionLogModel.p1_agent.in_(agent_types)) |
                    (SessionLogModel.p2_agent.in_(agent_types))
                )

            if limit:
                query = query.limit(limit)
            
            logs = query.all()
            
            data = []
            for log in logs:
                session_info = db_session.query(SessionLogModel).filter(
                    SessionLogModel.session_id == log.session_id
                ).first()
                data.append(self._build_game_log_row(log, session_info))
            
            df = pd.DataFrame([row.__dict__ for row in data])
            
            if min_games:
                df = self._filter_by_min_games(df, min_games)
            
            return df
    
    def _create_version_directory(self, version: str) -> Path:
        """Create version directory for dataset."""
        version_dir = self.export_path / version
        version_dir.mkdir(parents=True, exist_ok=True)
        return version_dir
    
    def _generate_filename(self, version: str) -> str:
        """Generate filename for dataset export."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        return f"game_logs_{version}_{timestamp}.parquet"
    
    def _write_parquet_file(self, df: pd.DataFrame, filepath: Path) -> None:
        """Write dataframe to parquet file."""
        df.to_parquet(
            filepath,
            engine="pyarrow",
            compression="snappy",
            index=False,
        )

    def _create_latest_symlink(self, version_dir, version, filename):
        latest_file = version_dir / f"game_logs_{version}_latest.parquet"
        target_file = version_dir / filename

        # Windows-safe: overwrite copy instead of symlink
        shutil.copyfile(target_file, latest_file)
    
    def export_to_parquet(
        self,
        version: str = "v1",
        limit: Optional[int] = None,
        agent_types: Optional[List[str]] = None,
        min_games: Optional[int] = None,
        tag: Optional[str] = None
    ) -> DatasetExportResult:
        """
        Export game logs to parquet file.
        
        Args:
            version: Dataset version (e.g., "v1", "v2")
            limit: Maximum number of logs to export
            agent_types: Filter by agent types
            min_games: Minimum number of games per agent type
            
        Returns:
            DatasetExportResult with export information
        """
        df = self.export_game_logs_to_dataframe(
            limit=limit,
            agent_types=agent_types,
            min_games=min_games,
            tag=tag,
        )
        
        if df.empty:
            raise ValueError("No data to export")
        
        version_dir = self._create_version_directory(version)
        filename = self._generate_filename(version)
        filepath = version_dir / filename
        
        self._write_parquet_file(df, filepath)
        dvc = DVCManager()

        # Ensure DVC is initialized (pick local or minio)
        if not dvc.is_initialized():
            dvc.initialize(remote_type="local")  # or "minio" if you configured it

        # Track the dataset file with DVC
        dvc_file = dvc.add_dataset(str(filepath), version=version)

        # Push to remote (optional but best for teammates)
        dvc.push_dataset(str(filepath))
        self._create_latest_symlink(version_dir, version, filename)
        
        file_size_mb = filepath.stat().st_size / (1024 * 1024)
        
        return DatasetExportResult(
            version=version,
            file_path=filepath,
            file_size_mb=round(file_size_mb, 2),
            total_records=len(df),
        )
    
    def _get_latest_file(self, version_dir: Path, version: str) -> Optional[Path]:
        """Get the latest parquet file for a version."""
        latest_file = version_dir / f"game_logs_{version}_latest.parquet"
        
        if latest_file.exists():
            return latest_file
        
        parquet_files = list(version_dir.glob("*.parquet"))
        if not parquet_files:
            return None
        
        return max(parquet_files, key=lambda p: p.stat().st_mtime)
    
    def _build_dataset_stats(self, df: pd.DataFrame, file_path: Path) -> DatasetStats:
        """Build dataset statistics from dataframe."""
        # Calculate agent distribution from both p1_agent and p2_agent columns
        agent_distribution = {}
        for agent_col in ['p1_agent', 'p2_agent']:
            if agent_col in df.columns:
                counts = df[agent_col].value_counts().to_dict()
                for agent, count in counts.items():
                    if agent:  # Skip None/null
                        agent_distribution[agent] = agent_distribution.get(agent, 0) + count

        # Calculate outcome distribution
        outcome_distribution = {}
        if 'final_result' in df.columns:
            outcome_distribution = df['final_result'].value_counts().to_dict()

        return DatasetStats(
            version="",
            file=str(file_path),
            total_records=len(df),
            unique_sessions=df["session_id"].nunique() if "session_id" in df.columns else 0,
            unique_tags=df["tag"].nunique() if "tag" in df.columns else 0,
            agent_distribution=agent_distribution,
            outcome_distribution=outcome_distribution,
            date_range={
                "min": df["timestamp"].min() if "timestamp" in df.columns else None,
                "max": df["timestamp"].max() if "timestamp" in df.columns else None,
            },
        )
    
    def _get_version_stats(self, version: str) -> Optional[DatasetStats]:
        """Get statistics for a specific version."""
        version_dir = self.export_path / version
        
        if not version_dir.exists():
            return None
        
        latest_file = self._get_latest_file(version_dir, version)
        if not latest_file:
            return None
        
        df = pd.read_parquet(latest_file)
        stats = self._build_dataset_stats(df, latest_file)
        stats.version = version
        return stats
    
    def get_dataset_stats(self, version: Optional[str] = None) -> Dict[str, DatasetStats]:
        """
        Get statistics about exported datasets.
        
        Args:
            version: Dataset version (None for all versions)
            
        Returns:
            Dictionary mapping version to DatasetStats
        """
        if version:
            stats = self._get_version_stats(version)
            return {version: stats} if stats else {}
        
        all_stats = {}
        for version_dir in self.export_path.iterdir():
            if not version_dir.is_dir():
                continue
            
            version_name = version_dir.name
            version_stats = self._get_version_stats(version_name)
            
            if version_stats:
                all_stats[version_name] = version_stats
        
        return all_stats
