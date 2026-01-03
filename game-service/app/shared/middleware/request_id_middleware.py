"""
Request ID middleware for FastAPI.
Adds a unique request ID to each request for tracing.
"""
import uuid
from typing import Callable
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp


class RequestIDMiddleware(BaseHTTPMiddleware):
    """Middleware to add a unique request ID to each request."""
    
    async def dispatch(self, request: Request, call_next: Callable):
        """Add request ID to request headers if not present."""
        # Check if request ID already exists
        request_id = request.headers.get("X-Request-ID")
        
        if not request_id:
            # Generate new request ID
            request_id = str(uuid.uuid4())
        
        # Add to request state for access in handlers
        request.state.request_id = request_id
        
        # Process request
        response = await call_next(request)
        
        # Add request ID to response headers
        response.headers["X-Request-ID"] = request_id
        
        return response
