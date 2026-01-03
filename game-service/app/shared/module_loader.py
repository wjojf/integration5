"""
Module Loader
Auto-discovers and loads modules using configuration.
"""
from typing import Dict, Optional, List, Tuple
from fastapi import APIRouter, FastAPI
import importlib
import os
from pathlib import Path

from app.config import settings


class ModuleLoader:
    """Handles automatic module discovery and loading."""
    
    def __init__(self, modules_base_path: str = "app.modules"):
        self.modules_base_path = modules_base_path
        self._loaded_modules: Dict[str, Dict] = {}
    
    def discover_modules(self) -> List[str]:
        """Auto-discover available modules from filesystem."""
        # Convert module path (e.g., "app.modules") to filesystem path
        # Use absolute path based on this file's location
        current_file = Path(__file__).resolve()
        project_root = current_file.parent.parent.parent

        # Convert dot notation to path
        relative_path = self.modules_base_path.replace(".", os.sep)
        modules_path = project_root / relative_path

        print(f"[ModuleLoader] Searching for modules in: {modules_path}")
        print(f"[ModuleLoader] Path exists: {modules_path.exists()}")

        if not modules_path.exists():
            return []

        modules = []
        try:
            for item in modules_path.iterdir():
                has_init = (item / "__init__.py").exists()
                is_valid = item.is_dir() and not item.name.startswith("__") and has_init
                print(f"[ModuleLoader] Found: {item.name} | is_dir: {item.is_dir()} | has __init__: {has_init} | valid: {is_valid}")
                if is_valid:
                    modules.append(item.name)
        except (OSError, PermissionError) as e:
            print(f"[ModuleLoader] Error reading modules directory: {e}")
            return []

        print(f"[ModuleLoader] Discovered modules: {modules}")
        return modules
    
    def load_module(self, module_name: str, api_prefix: str = "/api/v1") -> Optional[APIRouter]:
        """
        Load a single module.
        
        Returns:
            Router if module is enabled and loaded, None otherwise
        """
        # Check if module is enabled
        config_key = f"MODULE_{module_name.upper()}_ENABLED"
        if not getattr(settings, config_key, True):
            self._loaded_modules[module_name] = {"status": "disabled", "reason": "disabled_in_config"}
            return None
        
        try:
            module_path = f"{self.modules_base_path}.{module_name}"
            module = importlib.import_module(module_path)
            
            # Get module metadata
            metadata = {
                "name": getattr(module, "MODULE_NAME", module_name),
                "description": getattr(module, "MODULE_DESCRIPTION", ""),
                "version": getattr(module, "MODULE_VERSION", "unknown"),
            }
            
            # Setup module
            if hasattr(module, "setup_module"):
                router = module.setup_module(api_prefix=api_prefix)
                if router:
                    self._loaded_modules[module_name] = {
                        "status": "enabled",
                        **metadata,
                    }
                    return router
                else:
                    self._loaded_modules[module_name] = {
                        "status": "disabled",
                        "reason": "setup_returned_none",
                        **metadata,
                    }
                    return None
            elif hasattr(module, "router"):
                self._loaded_modules[module_name] = {
                    "status": "enabled",
                    **metadata,
                }
                return module.router
            else:
                self._loaded_modules[module_name] = {
                    "status": "error",
                    "reason": "no_setup_or_router",
                    **metadata,
                }
                return None
                
        except ImportError as e:
            self._loaded_modules[module_name] = {
                "status": "error",
                "reason": f"import_error: {str(e)}",
            }
            return None
        except Exception as e:
            self._loaded_modules[module_name] = {
                "status": "error",
                "reason": f"setup_error: {str(e)}",
            }
            return None

    def load_all_modules(self, app: FastAPI, api_prefix: str = "/api/v1") -> Dict[str, Dict]:
        modules = self.discover_modules()

        for module_name in modules:
            router = self.load_module(module_name, api_prefix=api_prefix)
            if router:
                # Mount router at api_prefix + /module_name
                # e.g., /api/v1/games, /api/v1/chatbot, etc.
                # Exception: games module mounts at api_prefix only (not api_prefix/games)
                # because its routes already include /games prefix
                if module_name == "games":
                    module_prefix = api_prefix
                else:
                    module_prefix = f"{api_prefix}/{module_name}"
                app.include_router(router, prefix=module_prefix)

        return self._loaded_modules

    def get_module_status(self) -> Dict[str, Dict]:
        """Get status of all modules."""
        return self._loaded_modules.copy()


# Global module loader instance
module_loader = ModuleLoader()


