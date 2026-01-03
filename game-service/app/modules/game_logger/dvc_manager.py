"""
DVC Manager - Simple Dataset Versioning
"""
import subprocess
import sys
from pathlib import Path
from dataclasses import dataclass
from typing import Optional

from app.config import settings


@dataclass
class DVCInitResult:
    """Result of DVC initialization."""
    initialized: bool
    message: str


class DVCManager:
    """Simple DVC manager for dataset versioning."""

    def __init__(self):
        # Prefer an explicit project root if you have one in settings,
        # otherwise auto-detect by walking upwards.
        self.repo_root = self._resolve_repo_root()
        self.dvc_dir = self.repo_root / ".dvc"

    def _resolve_repo_root(self) -> Path:
        # 1) If your settings defines PROJECT_ROOT, use it.
        project_root = getattr(settings, "PROJECT_ROOT", None)
        if project_root:
            return Path(project_root).resolve()

        # 2) Otherwise auto-detect root by walking up until we find docker-compose.yml or .git
        start = Path.cwd().resolve()
        for parent in [start] + list(start.parents):
            if (parent / "docker-compose.yml").exists() or (parent / ".git").exists():
                return parent
        return start

    def _dvc(self, *args: str) -> list[str]:
        """
        Call DVC via the current Python interpreter.
        This avoids PATH issues inside Docker (e.g. /root/.local/bin not in PATH).
        """
        return [sys.executable, "-m", "dvc", *args]

    def is_initialized(self) -> bool:
        """Check if DVC is initialized."""
        return self.dvc_dir.exists() and (self.dvc_dir / "config").exists()

    def initialize(
        self,
        remote_type: str = "local",
        remote_url: Optional[str] = None,
    ) -> DVCInitResult:
        """
        Initialize DVC with optional remote.

        Args:
            remote_type: "local" or "minio"
            remote_url: Remote URL (defaults to ./data/dvc_remote relative to repo root)
        """
        if self.is_initialized():
            return DVCInitResult(True, "DVC already initialized")

        # Initialize DVC (with SCM integration for proper versioning with Git)
        subprocess.run(self._dvc("init", "--no-scm"), cwd=self.repo_root, check=True)

        # Setup remote
        url = remote_url or "./data/dvc_remote"

        if remote_type == "local":
            # Ensure local remote directory exists (relative to repo root)
            (self.repo_root / url).mkdir(parents=True, exist_ok=True)
            subprocess.run(
                self._dvc("remote", "add", "-d", "local", url),
                cwd=self.repo_root,
                check=True,
            )

        elif remote_type == "minio":
            subprocess.run(
                self._dvc("remote", "add", "-d", "minio", url),
                cwd=self.repo_root,
                check=True,
            )
            # Configure MinIO endpoint
            if getattr(settings, "MINIO_ENDPOINT", None):
                subprocess.run(
                    self._dvc(
                        "remote",
                        "modify",
                        "minio",
                        "endpointurl",
                        f"http://{settings.MINIO_ENDPOINT}",
                    ),
                    cwd=self.repo_root,
                    check=True,
                )

        else:
            return DVCInitResult(False, f"Unknown remote_type: {remote_type}")

        return DVCInitResult(True, f"DVC initialized with {remote_type} remote")

    def add_dataset(self, file_path: str, version: str) -> str:
        """
        Add dataset file to DVC tracking.

        Args:
            file_path: Path to the parquet file
            version: Dataset version (v1, v2, etc.) - currently informational

        Returns:
            Path to .dvc file (string)
        """
        path = Path(file_path)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")

        subprocess.run(self._dvc("add", str(path)), cwd=self.repo_root, check=True)
        return f"{file_path}.dvc"

    def push_dataset(self, file_path: str) -> None:
        """
        Push tracked dataset content to configured remote.
        Accepts either the data file or the .dvc file.
        """
        dvc_file = f"{file_path}.dvc" if not file_path.endswith(".dvc") else file_path
        subprocess.run(self._dvc("push", dvc_file), cwd=self.repo_root, check=True)

    def pull_dataset(self, file_path: str) -> None:
        """
        Pull tracked dataset content from configured remote.
        Accepts either the data file or the .dvc file.
        """
        dvc_file = f"{file_path}.dvc" if not file_path.endswith(".dvc") else file_path
        subprocess.run(self._dvc("pull", dvc_file), cwd=self.repo_root, check=True)
