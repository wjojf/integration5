"""
Metrics middleware for FastAPI.
Tracks request metrics for monitoring and observability.
"""
import time
import logging
from typing import Callable
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger(__name__)


class MetricsMiddleware(BaseHTTPMiddleware):
    """Middleware to track request metrics."""
    
    def __init__(self, app, metrics_collector=None):
        super().__init__(app)
        self.metrics_collector = metrics_collector
        self._request_count = {}
        self._request_duration = {}
    
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """Track metrics for request."""
        start_time = time.time()
        method = request.method
        path = request.url.path
        
        # Increment request counter
        key = f"{method}:{path}"
        self._request_count[key] = self._request_count.get(key, 0) + 1
        
        try:
            response = await call_next(request)
            
            # Calculate duration
            duration = time.time() - start_time
            
            # Track duration
            if key not in self._request_duration:
                self._request_duration[key] = []
            self._request_duration[key].append(duration)
            
            # Keep only last 100 durations per endpoint
            if len(self._request_duration[key]) > 100:
                self._request_duration[key] = self._request_duration[key][-100:]
            
            # Add metrics headers
            response.headers["X-Request-Count"] = str(self._request_count[key])
            avg_duration = sum(self._request_duration[key]) / len(self._request_duration[key])
            response.headers["X-Avg-Duration"] = f"{avg_duration:.3f}"
            
            # Log metrics if collector available
            if self.metrics_collector:
                try:
                    self.metrics_collector.record_request(
                        method=method,
                        path=path,
                        status_code=response.status_code,
                        duration=duration
                    )
                except Exception as e:
                    logger.debug(f"Failed to record metrics: {e}")
            
            return response
            
        except Exception as e:
            duration = time.time() - start_time
            logger.error(f"Request failed: {method} {path} - {str(e)}")
            raise
    
    def get_metrics(self) -> dict:
        """Get current metrics snapshot."""
        metrics = {}
        for key, count in self._request_count.items():
            durations = self._request_duration.get(key, [])
            avg_duration = sum(durations) / len(durations) if durations else 0
            metrics[key] = {
                "count": count,
                "avg_duration": avg_duration,
                "min_duration": min(durations) if durations else 0,
                "max_duration": max(durations) if durations else 0
            }
        return metrics
