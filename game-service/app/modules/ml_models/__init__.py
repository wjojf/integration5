"""
ML Models Module - Connect Four Machine Learning

Clean architecture:
- models.py - DTOs and Pydantic schemas
- service.py - Business logic and model predictions
- api.py - FastAPI endpoints
- config.py - Configuration and hyperparameters
"""

from .api import router

__all__ = ["router"]
