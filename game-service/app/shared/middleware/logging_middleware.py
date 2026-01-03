"""
Request logging middleware for FastAPI.
Logs all incoming requests with timing, status codes, and error information.
"""
import time
import logging
from typing import Callable
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp

logger = logging.getLogger(__name__)


class LoggingMiddleware(BaseHTTPMiddleware):
    """Middleware to log all HTTP requests with timing and status information."""
    
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Process request and log information."""
        # Extract request information
        start_time = time.time()
        method = request.method
        path = request.url.path
        query_params = str(request.query_params) if request.query_params else ""
        client_ip = request.client.host if request.client else "unknown"
        user_agent = request.headers.get("user-agent", "unknown")
        
        # Get request ID if present
        request_id = request.headers.get("X-Request-ID", "N/A")
        
        # Log request start
        logger.info(
            f"Request started: {method} {path} | "
            f"IP: {client_ip} | "
            f"Request-ID: {request_id} | "
            f"Query: {query_params[:100] if query_params else 'None'}"
        )
        
        # Process request
        try:
            response = await call_next(request)
            
            # Calculate processing time
            process_time = time.time() - start_time
            
            # Extract response information
            status_code = response.status_code
            
            # Log response
            log_level = logging.INFO if status_code < 400 else logging.WARNING
            logger.log(
                log_level,
                f"Request completed: {method} {path} | "
                f"Status: {status_code} | "
                f"Time: {process_time:.3f}s | "
                f"Request-ID: {request_id}"
            )
            
            # Add timing header
            response.headers["X-Process-Time"] = f"{process_time:.3f}"
            response.headers["X-Request-ID"] = request_id
            
            return response
            
        except Exception as e:
            # Calculate processing time even on error
            process_time = time.time() - start_time
            
            # Log error
            logger.error(
                f"Request failed: {method} {path} | "
                f"Error: {str(e)} | "
                f"Time: {process_time:.3f}s | "
                f"Request-ID: {request_id}",
                exc_info=True
            )
            
            # Re-raise to let FastAPI handle it
            raise
