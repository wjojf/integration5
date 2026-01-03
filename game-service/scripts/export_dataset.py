# scripts/export_dataset.py

"""
Export Dataset Script
Exports game logs from database to parquet file.

Usage:
    python -m scripts.export_dataset v1
    python -m scripts.export_dataset v1 --limit 1000
    python -m scripts.export_dataset v1 --dvc
"""

import argparse
import subprocess
from pathlib import Path

from app.modules.game_logger.dataset_export import DatasetExportService
from app.modules.game_logger.service import GameLoggerService


def init_dvc(data_dir: Path) -> None:
    """Initialize DVC if not already done."""
    if not (data_dir / ".dvc").exists():
        print("Initializing DVC...")
        subprocess.run(["dvc", "init"], cwd=data_dir, check=True)

        # Setup local remote
        remote_path = data_dir / "dvc_remote"
        remote_path.mkdir(exist_ok=True)
        subprocess.run(
            ["dvc", "remote", "add", "-d", "local", str(remote_path)],
            cwd=data_dir,
            check=True,
        )
        print(f"DVC initialized with local remote: {remote_path}")


def add_to_dvc(file_path: Path, data_dir: Path) -> None:
    """Add file to DVC tracking."""
    subprocess.run(["dvc", "add", str(file_path)], cwd=data_dir, check=True)
    print(f"Added to DVC: {file_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Export game logs to parquet")
    parser.add_argument("version", help="Dataset version (v1, v2, etc.)")
    parser.add_argument("--limit", type=int, help="Max records to export")
    parser.add_argument(
        "--dvc", action="store_true", help="Add exported file to DVC tracking"
    )
    parser.add_argument("--tag", type=str, help="Export only sessions with this tag")

    args = parser.parse_args()

    # Create logger service and exporter
    logger_service = GameLoggerService()
    exporter = DatasetExportService(logger_service)

    print(f"Exporting dataset {args.version}...")
    result = exporter.export_to_parquet(
        version=args.version,
        limit=args.limit,
        agent_types=None,   # you can filter later if you want
        min_games=None,
        tag=args.tag,
    )

    print(f"\n✓ Exported: {result.file_path}")
    print(f"  Records: {result.total_records}")
    print(f"  Size: {result.file_size_mb} MB")

    # Show stats for this version
    stats_map = exporter.get_dataset_stats(args.version)
    stats = stats_map.get(args.version)
    if stats:
        print("\nDataset Stats:")
        print(f"  Unique games: {stats.unique_games}")
        print(f"  Unique players: {stats.unique_players}")
        print(f"  Agent types: {stats.agent_types}")
        print(f"  Date range: {stats.date_range}")

    # Optional: DVC tracking
    if args.dvc:
        data_dir = Path("./data")
        data_dir.mkdir(exist_ok=True)
        init_dvc(data_dir)
        add_to_dvc(result.file_path, data_dir)


if __name__ == "__main__":
    main()
