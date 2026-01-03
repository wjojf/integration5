"""
Shared Exception Handlers
Centralized exception handling for the application.
"""
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from typing import Union


class AppException(Exception):
    """Base application exception."""
    def __init__(self, message: str, status_code: int = 500):
        self.message = message
        self.status_code = status_code
        super().__init__(self.message)


class ValidationError(AppException):
    """Validation error exception."""
    def __init__(self, message: str):
        super().__init__(message, status_code=400)


class NotFoundError(AppException):
    """Resource not found exception."""
    def __init__(self, message: str):
        super().__init__(message, status_code=404)


class ServiceError(AppException):
    """Service error exception."""
    def __init__(self, message: str, status_code: int = 500):
        super().__init__(message, status_code)


async def app_exception_handler(request: Request, exc: AppException) -> JSONResponse:
    """Handle application exceptions."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.message, "type": exc.__class__.__name__},
    )


async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    """Handle HTTP exceptions."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.detail, "type": "HTTPException"},
    )


async def value_error_handler(request: Request, exc: ValueError) -> JSONResponse:
    """Handle ValueError exceptions."""
    return JSONResponse(
        status_code=400,
        content={"error": str(exc), "type": "ValidationError"},
    )


