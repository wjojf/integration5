"""
Comprehensive health check system for game service.
Checks database, RabbitMQ, Redis, and module health.
"""
import logging
from enum import Enum
from typing import Dict, Optional, Any
from datetime import datetime

from app.config import settings
from app.shared.messaging import get_rabbitmq_client

logger = logging.getLogger(__name__)


class HealthStatus(str, Enum):
    """Health status enumeration."""
    HEALTHY = "healthy"
    UNHEALTHY = "unhealthy"
    DEGRADED = "degraded"


class ComponentHealth:
    """Represents the health of a component."""
    
    def __init__(
        self,
        name: str,
        status: HealthStatus,
        message: Optional[str] = None,
        details: Optional[Dict[str, Any]] = None
    ):
        self.name = name
        self.status = status
        self.message = message
        self.details = details or {}
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "name": self.name,
            "status": self.status.value,
            "message": self.message,
            "details": self.details
        }


class HealthChecker:
    """Comprehensive health checker for all service components."""
    
    def __init__(self):
        self._db_health: Optional[ComponentHealth] = None
        self._rabbitmq_health: Optional[ComponentHealth] = None
        self._redis_health: Optional[ComponentHealth] = None
    
    def check_database(self) -> ComponentHealth:
        """Check database connectivity."""
        try:
            from sqlalchemy import create_engine, text
            from sqlalchemy.exc import SQLAlchemyError
            
            # Create a test connection
            engine = create_engine(
                settings.DATABASE_URL,
                connect_args={"connect_timeout": 2},
                pool_pre_ping=True  # Enable connection health checks
            )
            
            # Try a simple query
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            
            return ComponentHealth(
                name="database",
                status=HealthStatus.HEALTHY,
                message="Database connection is healthy",
                details={
                    "host": settings.DB_HOST,
                    "port": settings.DB_PORT,
                    "database": settings.DB_NAME
                }
            )
        except SQLAlchemyError as e:
            logger.error(f"Database health check failed: {e}", exc_info=True)
            return ComponentHealth(
                name="database",
                status=HealthStatus.UNHEALTHY,
                message=f"Database connection failed: {str(e)}",
                details={
                    "host": settings.DB_HOST,
                    "port": settings.DB_PORT,
                    "database": settings.DB_NAME
                }
            )
        except Exception as e:
            logger.error(f"Database health check error: {e}", exc_info=True)
            return ComponentHealth(
                name="database",
                status=HealthStatus.UNHEALTHY,
                message=f"Database check error: {str(e)}",
            )
    
    def check_rabbitmq(self) -> ComponentHealth:
        """Check RabbitMQ connectivity."""
        try:
            rabbitmq_client = get_rabbitmq_client()
            rabbitmq_client.connect()
            
            # Check if connection is active
            if rabbitmq_client._connected:
                return ComponentHealth(
                    name="rabbitmq",
                    status=HealthStatus.HEALTHY,
                    message="RabbitMQ connection is healthy",
                    details={
                        "host": settings.RABBITMQ_HOST,
                        "port": settings.RABBITMQ_PORT,
                        "exchange": settings.RABBITMQ_EXCHANGE_NAME
                    }
                )
            else:
                return ComponentHealth(
                    name="rabbitmq",
                    status=HealthStatus.UNHEALTHY,
                    message="RabbitMQ connection is not active",
                )
        except Exception as e:
            logger.error(f"RabbitMQ health check failed: {e}", exc_info=True)
            return ComponentHealth(
                name="rabbitmq",
                status=HealthStatus.UNHEALTHY,
                message=f"RabbitMQ connection failed: {str(e)}",
                details={
                    "host": settings.RABBITMQ_HOST,
                    "port": settings.RABBITMQ_PORT
                }
            )
    
    def check_redis(self) -> ComponentHealth:
        """Check Redis connectivity (if enabled)."""
        if not settings.REDIS_ENABLED:
            return ComponentHealth(
                name="redis",
                status=HealthStatus.HEALTHY,
                message="Redis is disabled",
                details={"enabled": False}
            )
        
        try:
            import redis
            client = redis.Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                password=settings.REDIS_PASSWORD,
                db=settings.REDIS_DB,
                socket_connect_timeout=2
            )
            # Ping Redis
            client.ping()
            
            return ComponentHealth(
                name="redis",
                status=HealthStatus.HEALTHY,
                message="Redis connection is healthy",
                details={
                    "host": settings.REDIS_HOST,
                    "port": settings.REDIS_PORT,
                    "db": settings.REDIS_DB
                }
            )
        except ImportError:
            return ComponentHealth(
                name="redis",
                status=HealthStatus.DEGRADED,
                message="Redis client not installed",
            )
        except Exception as e:
            logger.error(f"Redis health check failed: {e}", exc_info=True)
            return ComponentHealth(
                name="redis",
                status=HealthStatus.UNHEALTHY,
                message=f"Redis connection failed: {str(e)}",
                details={
                    "host": settings.REDIS_HOST,
                    "port": settings.REDIS_PORT
                }
            )
    
    def check_all(self) -> Dict[str, Any]:
        """Check all components and return comprehensive health status."""
        components = {
            "database": self.check_database(),
            "rabbitmq": self.check_rabbitmq(),
            "redis": self.check_redis(),
        }
        
        # Determine overall status
        statuses = [comp.status for comp in components.values()]
        if HealthStatus.UNHEALTHY in statuses:
            overall_status = HealthStatus.UNHEALTHY
        elif HealthStatus.DEGRADED in statuses:
            overall_status = HealthStatus.DEGRADED
        else:
            overall_status = HealthStatus.HEALTHY
        
        return {
            "status": overall_status.value,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "components": {
                name: comp.to_dict() for name, comp in components.items()
            },
            "version": settings.APP_VERSION
        }
    
    def check_ready(self) -> Dict[str, Any]:
        """Check if service is ready to accept traffic."""
        # Ready means critical components are healthy
        db_health = self.check_database()
        rabbitmq_health = self.check_rabbitmq()
        
        ready = (
            db_health.status == HealthStatus.HEALTHY and
            rabbitmq_health.status == HealthStatus.HEALTHY
        )
        
        return {
            "ready": ready,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "components": {
                "database": db_health.to_dict(),
                "rabbitmq": rabbitmq_health.to_dict(),
            }
        }
    
    def check_live(self) -> Dict[str, Any]:
        """Check if service is alive (liveness probe)."""
        # Liveness just checks if the service is running
        return {
            "alive": True,
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }


# Global health checker instance
health_checker = HealthChecker()
