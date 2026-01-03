"""
Game session event publisher.
Publishes events to RabbitMQ when game sessions change.
"""
import logging
from typing import Dict, Any, Optional
from datetime import datetime

from app.config import settings
from app.shared.messaging import get_rabbitmq_client

logger = logging.getLogger(__name__)


def create_event_publisher() -> Optional[callable]:
    """Create an event publisher function for game session service."""
    try:
        rabbitmq_client = get_rabbitmq_client()

        def publish_event(routing_key: str, event: Dict[str, Any]) -> None:
            """Publish event to RabbitMQ."""
            try:
                rabbitmq_client.publish_event(
                    routing_key=routing_key,
                    event=event
                )
                logger.debug(f"Published event: {routing_key}")
            except Exception as e:
                logger.error(f"Failed to publish event {routing_key}: {e}", exc_info=True)

        return publish_event
    except Exception as e:
        logger.warning(f"Event publisher not available: {e}")
        return None

