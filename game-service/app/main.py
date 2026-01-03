"""
Main FastAPI application entry point.
Modular monolith with auto-discovery and dependency injection.
"""
import logging
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.config import settings
from app.shared.module_loader import module_loader
from app.shared.exceptions import (
    AppException,
    ValidationError,
    NotFoundError,
    ServiceError,
    app_exception_handler,
    http_exception_handler,
    value_error_handler,
)
from app.shared.middleware.logging_middleware import LoggingMiddleware
from app.shared.middleware.request_id_middleware import RequestIDMiddleware
from app.shared.middleware.security_headers_middleware import SecurityHeadersMiddleware
from app.shared.health.health_checker import health_checker

# Configure logging
logging.basicConfig(
    level=logging.INFO if not settings.DEBUG else logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title=settings.APP_NAME,
        version=settings.APP_VERSION,
        debug=settings.DEBUG,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    
    # Register exception handlers
    app.add_exception_handler(AppException, app_exception_handler)
    app.add_exception_handler(ValidationError, app_exception_handler)
    app.add_exception_handler(NotFoundError, app_exception_handler)
    app.add_exception_handler(ServiceError, app_exception_handler)
    app.add_exception_handler(ValueError, value_error_handler)
    
    # Middleware order matters - add in reverse order of execution
    # SecurityHeadersMiddleware runs first (outermost) - adds security headers
    app.add_middleware(SecurityHeadersMiddleware)
    
    # RequestIDMiddleware runs after security headers
    app.add_middleware(RequestIDMiddleware)
    
    # LoggingMiddleware runs after request ID is set
    app.add_middleware(LoggingMiddleware)

    # CORS middleware disabled - API Gateway handles CORS
    # Gateway's globalcors configuration provides CORS headers for all services
    # Enabling CORS here causes duplicate Access-Control-Allow-Origin headers
    # app.add_middleware(
    #     CORSMiddleware,
    #     allow_origins=settings.CORS_ORIGINS,
    #     allow_credentials=settings.CORS_ALLOW_CREDENTIALS,
    #     allow_methods=settings.CORS_ALLOW_METHODS,
    #     allow_headers=settings.CORS_ALLOW_HEADERS,
    # )
    
    # Auto-load all modules
    loaded_modules = module_loader.load_all_modules(app, api_prefix=settings.API_PREFIX)
    
    # Store loaded_modules in app state for access in lifecycle events
    app.state.loaded_modules = loaded_modules
    
    @app.get("/", tags=["System"])
    async def root():
        """Root endpoint with module status."""
        return {
            "name": settings.APP_NAME,
            "version": settings.APP_VERSION,
            "status": "running",
            "modules": loaded_modules,
        }
    
    @app.get("/health", tags=["Health"])
    async def health_check():
        """
        Comprehensive health check endpoint.
        Checks database, RabbitMQ, Redis, and module status.
        """
        return health_checker.check_all()
    
    @app.get("/health/ready", tags=["Health"])
    async def readiness_check():
        """
        Readiness probe endpoint.
        Returns 200 if service is ready to accept traffic.
        """
        ready_status = health_checker.check_ready()
        status_code = 200 if ready_status["ready"] else 503
        return JSONResponse(content=ready_status, status_code=status_code)
    
    @app.get("/health/live", tags=["Health"])
    async def liveness_check():
        """
        Liveness probe endpoint.
        Returns 200 if service is alive.
        """
        return health_checker.check_live()
    
    @app.get("/modules", tags=["System"])
    async def list_modules():
        """List all available modules and their status."""
        return {
            "modules": module_loader.get_module_status(),
        }
    
    @app.get("/metrics", tags=["System"])
    async def get_metrics():
        """
        Get request metrics.
        Returns request counts and duration statistics per endpoint.
        """
        # This would integrate with a metrics collector if available
        return {
            "message": "Metrics endpoint - integrate with Prometheus/StatsD for production",
            "note": "Basic metrics available via logging middleware"
        }
    
    # Add startup and shutdown events
    @app.on_event("startup")
    async def startup_event():
        """Application startup event."""
        modules = app.state.loaded_modules if hasattr(app.state, 'loaded_modules') else {}
        logger.info(f"Starting {settings.APP_NAME} v{settings.APP_VERSION}")
        logger.info(f"Loaded {len(modules)} modules: {list(modules.keys())}")
    
    @app.on_event("shutdown")
    async def shutdown_event():
        """Application shutdown event."""
        logger.info(f"Shutting down {settings.APP_NAME}")
    
    return app


# Create app instance
app = create_app()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.DEBUG,
    )
