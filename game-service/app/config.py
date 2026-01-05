"""
Configuration management for the game service.
Loads environment variables from .env file.
"""
import os
from typing import Optional, List
from pydantic_settings import BaseSettings, SettingsConfigDict



class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )
    
    # Application
    APP_NAME: str = "Game Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    API_PREFIX: str = "/api/v1"
    
    # Module Configuration
    MODULE_GAMES_ENABLED: bool = True
    MODULE_AI_PLAYER_ENABLED: bool = True
    MODULE_GAME_LOGGER_ENABLED: bool = True
    MODULE_CHATBOT_ENABLED: bool = True
    MODULE_ML_MODELS_ENABLED: bool = True
    
    # Database (PostgreSQL) - using platform-backend postgres
    DB_HOST: str = "banditgames-postgres"
    DB_PORT: int = 5432
    DB_NAME: str = "banditgames"
    DB_USER: str = "postgres"
    DB_PASSWORD: str = "postgres"
    DB_POOL_SIZE: int = 10
    DB_MAX_OVERFLOW: int = 20
    
    @property
    def DATABASE_URL(self) -> str:
        """Construct PostgreSQL connection URL."""
        return f"postgresql://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
    
    # AI Player Configuration
    AI_PLAYER_MCTS_ITERATIONS_LOW: int = 100
    AI_PLAYER_MCTS_ITERATIONS_MEDIUM: int = 500
    AI_PLAYER_MCTS_ITERATIONS_HIGH: int = 2000
    AI_PLAYER_MCTS_ITERATIONS_VERY_HIGH: int = 5000
    AI_PLAYER_TIMEOUT_SECONDS: float = 5.0
    AI_PLAYER_ENABLE_DYNAMIC_DIFFICULTY: bool = True
    AI_PLAYER_DIFFICULTY_ADJUSTMENT_THRESHOLD: float = 0.7  # Win rate threshold
    
    # Game Logger Configuration
    LOGGER_ENABLED: bool = True
    LOGGER_BATCH_SIZE: int = 100
    LOGGER_FLUSH_INTERVAL_SECONDS: int = 5
    
    # Dataset Export Configuration
    DATASET_EXPORT_ENABLED: bool = True
    DATASET_EXPORT_PATH: str = "./data/datasets"
    DATASET_EXPORT_FORMAT: str = "parquet"
    
    # DVC Configuration (for dataset versioning)
    DVC_ENABLED: bool = False
    DVC_REMOTE_TYPE: str = "local"  # local, s3, minio
    DVC_REMOTE_URL: str = "./data/dvc_remote"
    
    # MinIO Configuration (for DVC remote storage)
    MINIO_ENABLED: bool = False
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: Optional[str] = None
    MINIO_SECRET_KEY: Optional[str] = None
    MINIO_BUCKET_NAME: str = "game-datasets"
    MINIO_SECURE: bool = False
    
    # Chatbot Configuration
    CHATBOT_ENABLED: bool = True
    CHATBOT_LLM_PROVIDER: str = "openai"  # openai, anthropic, etc.
    CHATBOT_LLM_MODEL: str = "gpt-4"
    CHATBOT_LLM_API_KEY: Optional[str] = None
    CHATBOT_LLM_TEMPERATURE: float = 0.7
    CHATBOT_LLM_MAX_TOKENS: int = 500
    CHATBOT_VECTOR_DB_TYPE: str = "chroma"  # chroma, pinecone, etc.
    CHATBOT_VECTOR_DB_PATH: str = "./data/vector_db"
    CHATBOT_EMBEDDING_MODEL: str = "text-embedding-ada-002"
    CHATBOT_CACHE_ENABLED: bool = True
    CHATBOT_CACHE_TTL_SECONDS: int = 3600
    CHATBOT_RAG_TOP_K: int = 3

    # OpenAI / LLM configuration
    OPENAI_API_KEY: str | None = None
    OPENAI_CHAT_MODEL: str = "gpt-4o-mini"

    # Whether the chatbot should actually call the LLM or use placeholder
    CHATBOT_USE_LLM: bool = False
    
    # Platform Backend / API Gateway Configuration (for chatbot user context)
    JAVA_BACKEND_BASE_URL: Optional[str] = None  # API Gateway URL (defaults to http://127.0.0.1:8080 if not set)
    PLATFORM_BACKEND_URL: Optional[str] = None  # Direct platform backend URL (optional)
    SERVICE_API_KEY: Optional[str] = None  # API key for service-to-service authentication

    # ML Model Configuration (for future use)
    ML_MODEL_API_HOST: str = "localhost"
    ML_MODEL_API_PORT: int = 8001
    ML_MODEL_POLICY_ENDPOINT: str = "/predict/move"
    ML_MODEL_WIN_PROBABILITY_ENDPOINT: str = "/predict/win-probability"
    ML_MODEL_TIMEOUT_SECONDS: float = 2.0
    
    # MLFlow Configuration
    MLFLOW_TRACKING_URI: str = "http://localhost:5000"
    MLFLOW_EXPERIMENT_NAME: str = "game-service-ml"
    
    # Monitoring Configuration
    MONITORING_ENABLED: bool = True
    MONITORING_PROVIDER: str = "wandb"  # wandb, tensorboard
    MONITORING_PROJECT_NAME: str = "game-service"
    MONITORING_API_KEY: Optional[str] = None
    
    # RabbitMQ Configuration (for platform integration)
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = "admin"
    RABBITMQ_PASSWORD: str = "admin"
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_EXCHANGE_NAME: str = "game_events"
    RABBITMQ_CONNECTION_TIMEOUT: int = 60
    
    # RabbitMQ Dead Letter Exchange Configuration
    RABBITMQ_DLX_NAME: str = "game_events_dlx"
    RABBITMQ_DLQ_NAME: str = "game_events_dlq"
    
    # RabbitMQ Queue Names
    RABBITMQ_QUEUE_MOVE_REQUESTS: str = "game.move.requests"
    RABBITMQ_QUEUE_MOVE_RESPONSES: str = "game.move.responses"
    RABBITMQ_QUEUE_STATE_UPDATES: str = "game.state.updates"
    RABBITMQ_QUEUE_ACHIEVEMENTS: str = "game.achievements"
    RABBITMQ_QUEUE_SESSION_STARTED: str = "game.session.started"
    RABBITMQ_QUEUE_SESSION_ENDED: str = "game.session.ended"
    
    # RabbitMQ Routing Keys
    RABBITMQ_ROUTING_KEY_MOVE_REQUEST: str = "game.move.request"
    RABBITMQ_ROUTING_KEY_MOVE_RESPONSE: str = "game.move.response"
    RABBITMQ_ROUTING_KEY_STATE_UPDATED: str = "game.state.updated"
    RABBITMQ_ROUTING_KEY_ACHIEVEMENT_UNLOCKED: str = "game.achievement.unlocked"
    RABBITMQ_ROUTING_KEY_SESSION_STARTED: str = "game.session.started"
    RABBITMQ_ROUTING_KEY_SESSION_ENDED: str = "game.session.ended"
    
    @property
    def RABBITMQ_URL(self) -> str:
        """Construct RabbitMQ connection URL."""
        return f"amqp://{self.RABBITMQ_USER}:{self.RABBITMQ_PASSWORD}@{self.RABBITMQ_HOST}:{self.RABBITMQ_PORT}{self.RABBITMQ_VHOST}"
    
    # Redis Configuration (for caching)
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: Optional[str] = None
    REDIS_DB: int = 0
    REDIS_ENABLED: bool = False
    
    @property
    def REDIS_URL(self) -> Optional[str]:
        """Construct Redis connection URL."""
        if not self.REDIS_ENABLED:
            return None
        auth = f":{self.REDIS_PASSWORD}@" if self.REDIS_PASSWORD else ""
        return f"redis://{auth}{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"
    
    # CORS Configuration
    CORS_ORIGINS: List[str] = ["http://localhost:3000", "http://localhost:8080"]
    CORS_ALLOW_CREDENTIALS: bool = True
    CORS_ALLOW_METHODS: List[str] = ["*"]
    CORS_ALLOW_HEADERS: List[str] = ["*"]
    
    # Security
    SECRET_KEY: str = "change-me-in-production"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    
    # class Config:
    #     case_sensitive = True


# Global settings instance
settings = Settings()

