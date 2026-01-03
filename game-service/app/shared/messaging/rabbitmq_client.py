import json
import logging
import threading
from typing import Optional, Callable, Dict, Any
from contextlib import contextmanager

import pika
from pika.connection import ConnectionParameters
from pika.channel import Channel
from pika.exceptions import AMQPConnectionError, AMQPChannelError

from app.config import settings

# Dead letter exchange configuration (must match platform-backend)
DEAD_LETTER_EXCHANGE = "game_events_dlx"
DEAD_LETTER_ROUTING_KEY = "game_events_dlq"

logger = logging.getLogger(__name__)


class RabbitMQClient:
    def __init__(self) -> None:
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[Channel] = None
        self.exchange_name: str = settings.RABBITMQ_EXCHANGE_NAME
        self._connected: bool = False
        self._lock = threading.Lock()  # Lock for thread-safe operations
    
    def connect(self) -> None:
        """Connect to RabbitMQ. Thread-safe."""
        with self._lock:
            if self._connected and self.connection and not self.connection.is_closed:
                return
            
            try:
                params = pika.URLParameters(settings.RABBITMQ_URL)
                self.connection = pika.BlockingConnection(params)
                self.channel = self.connection.channel()
                
                self.channel.exchange_declare(
                    exchange=self.exchange_name,
                    exchange_type='topic',
                    durable=True
                )
                
                self._connected = True
                logger.info(
                    f"Connected to RabbitMQ: {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}"
                )
            except AMQPConnectionError as e:
                logger.error(f"Failed to connect to RabbitMQ: {e}")
                self._connected = False
                raise
            except Exception as e:
                logger.error(f"Unexpected error connecting to RabbitMQ: {e}")
                self._connected = False
                raise
    
    def disconnect(self) -> None:
        try:
            if self.channel and not self.channel.is_closed:
                self.channel.close()
            if self.connection and not self.connection.is_closed:
                self.connection.close()
            self._connected = False
            logger.info("Disconnected from RabbitMQ")
        except Exception as e:
            logger.warning(f"Error disconnecting from RabbitMQ: {e}")
    
    def ensure_connected(self) -> None:
        if (not self._connected or 
            not self.connection or 
            self.connection.is_closed or
            not self.channel or
            self.channel.is_closed):
            self.connect()
    
    def publish_event(
        self,
        routing_key: str,
        event: Dict[str, Any],
        persistent: bool = True
    ) -> None:
        self.ensure_connected()
        
        try:
            message = json.dumps(event, default=str)
            
            timestamp_ms: Optional[int] = None
            event_timestamp = event.get('timestamp')
            if hasattr(event_timestamp, 'timestamp'):
                timestamp_ms = int(event_timestamp.timestamp() * 1000)
            elif isinstance(event_timestamp, str):
                try:
                    from datetime import datetime
                    dt = datetime.fromisoformat(event_timestamp.replace('Z', '+00:00'))
                    timestamp_ms = int(dt.timestamp() * 1000)
                except (ValueError, AttributeError):
                    pass
            
            properties = pika.BasicProperties(
                delivery_mode=2 if persistent else 1,
                content_type='application/json',
                timestamp=timestamp_ms
            )
            
            self.channel.basic_publish(
                exchange=self.exchange_name,
                routing_key=routing_key,
                body=message,
                properties=properties,
                mandatory=True
            )
            
            logger.debug(f"Published event with routing key: {routing_key}")
        except AMQPConnectionError as e:
            logger.error(f"Connection error while publishing event: {e}")
            self._connected = False
            raise
        except AMQPChannelError as e:
            logger.error(f"Channel error while publishing event: {e}")
            raise
        except Exception as e:
            logger.error(f"Unexpected error publishing event: {e}")
            raise
    
    def declare_queue(
        self,
        queue_name: str,
        durable: bool = True,
        exclusive: bool = False,
        auto_delete: bool = False,
        dead_letter_exchange: Optional[str] = None,
        dead_letter_routing_key: Optional[str] = None,
        passive: bool = False
    ) -> None:
        """
        Declare a queue. If passive=True, only check if queue exists without creating it.
        If queue already exists with different arguments, this will use the existing queue.
        
        Strategy: First try to declare passively to check if queue exists.
        If it exists, use it as-is. If not, declare it with desired arguments.
        This avoids PRECONDITION_FAILED errors.
        """
        if passive:
            # Passive declaration - just check if queue exists
            self.ensure_connected()
            try:
                self.channel.queue_declare(queue=queue_name, passive=True)
                logger.debug(f"Queue {queue_name} exists (passive check)")
                return
            except pika.exceptions.ChannelClosedByBroker as e:
                if "NOT_FOUND" in str(e):
                    raise  # Queue doesn't exist
                # Queue exists but channel was closed for other reason
                self._force_reconnect()
                self.channel.queue_declare(queue=queue_name, passive=True)
                return
            except Exception as e:
                logger.error(f"Error in passive queue declaration for {queue_name}: {e}")
                raise
        
        # Active declaration - check if queue exists first, then create if needed
        max_retries = 3
        retry_count = 0
        
        while retry_count < max_retries:
            try:
                self.ensure_connected()
                
                # First, try to check if queue exists (passive declaration)
                queue_exists = False
                try:
                    self.channel.queue_declare(queue=queue_name, passive=True)
                    # Queue exists - use it as-is, don't try to modify it
                    logger.info(f"Using existing queue: {queue_name}")
                    return
                except pika.exceptions.ChannelClosedByBroker as e:
                    error_str = str(e)
                    if "NOT_FOUND" in error_str or "404" in error_str:
                        # Queue doesn't exist, channel was closed, need to reconnect
                        # This is expected - we'll create the queue below
                        queue_exists = False
                        self._force_reconnect()
                        self.ensure_connected()
                    else:
                        # Channel closed for other reason, reconnect and retry
                        retry_count += 1
                        if retry_count >= max_retries:
                            logger.error(f"Failed to check queue {queue_name} after {max_retries} retries: {e}")
                            raise
                        logger.warning(f"Channel closed, retry {retry_count}/{max_retries} for queue {queue_name}: {e}")
                        self._force_reconnect()
                        continue
                except Exception as e:
                    # Other error - assume queue doesn't exist, but reconnect to be safe
                    logger.debug(f"Error checking queue existence for {queue_name}: {e}, will try to create")
                    queue_exists = False
                    self._force_reconnect()
                    self.ensure_connected()
                
                # Queue doesn't exist, create it with desired arguments
                arguments = {}
                if dead_letter_exchange:
                    arguments['x-dead-letter-exchange'] = dead_letter_exchange
                if dead_letter_routing_key:
                    arguments['x-dead-letter-routing-key'] = dead_letter_routing_key
                
                try:
                    self.channel.queue_declare(
                        queue=queue_name,
                        durable=durable,
                        exclusive=exclusive,
                        auto_delete=auto_delete,
                        arguments=arguments if arguments else None
                    )
                    logger.debug(f"Created queue: {queue_name} (DLX: {dead_letter_exchange})")
                    return  # Success, exit retry loop
                except pika.exceptions.ChannelClosedByBroker as create_error:
                    # Queue exists with different arguments - use existing queue
                    if "PRECONDITION_FAILED" in str(create_error):
                        logger.warning(f"Queue {queue_name} exists with different arguments, using existing queue")
                        self._force_reconnect()
                        self.ensure_connected()
                        # Verify it exists by declaring passively
                        try:
                            self.channel.queue_declare(queue=queue_name, passive=True)
                            logger.info(f"Using existing queue: {queue_name}")
                            return
                        except Exception as passive_error:
                            retry_count += 1
                            if retry_count >= max_retries:
                                logger.error(f"Queue {queue_name} cannot be used after {max_retries} retries: {passive_error}")
                                raise
                            logger.warning(f"Retry {retry_count}/{max_retries} for queue {queue_name}: {passive_error}")
                            continue
                    else:
                        # Other channel closure error, reconnect and retry
                        raise
                
            except pika.exceptions.ChannelClosedByBroker as e:
                # Channel was closed - reconnect and retry
                error_str = str(e)
                if "PRECONDITION_FAILED" in error_str:
                    logger.warning(f"Queue {queue_name} exists with different arguments, using existing queue")
                    self._force_reconnect()
                    self.ensure_connected()
                    # Try passive declaration to verify it exists
                    try:
                        self.channel.queue_declare(queue=queue_name, passive=True)
                        logger.info(f"Using existing queue: {queue_name}")
                        return
                    except Exception as passive_error:
                        retry_count += 1
                        if retry_count >= max_retries:
                            logger.error(f"Queue {queue_name} cannot be used after {max_retries} retries: {passive_error}")
                            raise
                        logger.warning(f"Retry {retry_count}/{max_retries} for queue {queue_name}: {passive_error}")
                        continue
                else:
                    retry_count += 1
                    if retry_count >= max_retries:
                        logger.error(f"Error declaring queue {queue_name} after {max_retries} retries: {e}")
                        raise
                    logger.warning(f"Channel closed, retry {retry_count}/{max_retries} for queue {queue_name}: {e}")
                    self._force_reconnect()
                    continue
            except (pika.exceptions.StreamLostError, pika.exceptions.ChannelWrongStateError) as e:
                # Connection/channel is in a bad state, reconnect
                retry_count += 1
                if retry_count >= max_retries:
                    logger.error(f"Failed to declare queue {queue_name} after {max_retries} retries: {e}")
                    raise
                logger.warning(f"Connection issue, retry {retry_count}/{max_retries} for queue {queue_name}: {e}")
                self._force_reconnect()
                continue
            except Exception as e:
                logger.error(f"Error declaring queue {queue_name}: {e}")
                raise
    
    def _force_reconnect(self) -> None:
        """Force a full reconnection by closing and clearing all connection state.
        Thread-safe: uses a lock to prevent concurrent reconnections."""
        with self._lock:
            self._connected = False
            if self.channel:
                try:
                    if not self.channel.is_closed:
                        self.channel.close()
                except Exception:
                    pass
            if self.connection:
                try:
                    if not self.connection.is_closed:
                        self.connection.close()
                except Exception:
                    pass
            self.channel = None
            self.connection = None
            # Small delay to let connection fully close
            import time
            time.sleep(0.2)
    
    def bind_queue(
        self,
        queue_name: str,
        routing_key: str
    ) -> None:
        max_retries = 3
        retry_count = 0
        
        while retry_count < max_retries:
            try:
                self.ensure_connected()
                
                self.channel.queue_bind(
                    exchange=self.exchange_name,
                    queue=queue_name,
                    routing_key=routing_key
                )
                logger.debug(f"Bound queue {queue_name} with routing key: {routing_key}")
                return  # Success, exit retry loop
            except (pika.exceptions.StreamLostError, 
                    pika.exceptions.ChannelWrongStateError,
                    pika.exceptions.ChannelClosedByBroker) as e:
                # Connection/channel is in a bad state, reconnect
                retry_count += 1
                if retry_count >= max_retries:
                    logger.error(f"Failed to bind queue {queue_name} after {max_retries} retries: {e}")
                    raise
                logger.warning(f"Connection issue, retry {retry_count}/{max_retries} for binding queue {queue_name}: {e}")
                self._force_reconnect()
                continue
            except Exception as e:
                logger.error(f"Error binding queue {queue_name}: {e}")
                raise
    
    def consume_queue(
        self,
        queue_name: str,
        routing_key: str,
        callback: Callable[[Dict[str, Any]], None],
        auto_ack: bool = False,
        use_dead_letter: bool = True
    ) -> None:
        self.ensure_connected()
        
        # Declare queue with dead letter exchange if enabled (matches platform-backend configuration)
        self.declare_queue(
            queue_name,
            dead_letter_exchange=DEAD_LETTER_EXCHANGE if use_dead_letter else None,
            dead_letter_routing_key=DEAD_LETTER_ROUTING_KEY if use_dead_letter else None
        )
        self.bind_queue(queue_name, routing_key)
        
        # Ensure channel is still open before consuming
        self.ensure_connected()
        if not self.channel or self.channel.is_closed:
            logger.warning("Channel closed after binding, reconnecting...")
            self._force_reconnect()
            self.ensure_connected()
        
        def on_message(ch: Any, method: Any, properties: Any, body: bytes) -> None:
            try:
                event = json.loads(body)
                callback(event)
                
                if not auto_ack:
                    ch.basic_ack(delivery_tag=method.delivery_tag)
            except json.JSONDecodeError as e:
                logger.error(f"Failed to decode message: {e}")
                if not auto_ack:
                    ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            except Exception as e:
                logger.error(f"Error processing message: {e}")
                if not auto_ack:
                    ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
        
        try:
            self.channel.basic_consume(
                queue=queue_name,
                on_message_callback=on_message,
                auto_ack=auto_ack
            )
            
            logger.info(f"Started consuming queue: {queue_name} with routing key: {routing_key}")
            self.channel.start_consuming()
        except Exception as e:
            logger.error(f"Error consuming from queue {queue_name}: {e}")
            raise
    
    @contextmanager
    def get_channel(self) -> Any:
        self.ensure_connected()
        try:
            yield self.channel
        except Exception as e:
            logger.error(f"Error in channel context: {e}")
            raise


_rabbitmq_client: Optional[RabbitMQClient] = None


def get_rabbitmq_client() -> RabbitMQClient:
    global _rabbitmq_client
    if _rabbitmq_client is None:
        _rabbitmq_client = RabbitMQClient()
    return _rabbitmq_client
