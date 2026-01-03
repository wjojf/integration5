# Game Service Structure Documentation

## Overview

The Game Service is a **modular monolith** built with FastAPI that follows a clean architecture pattern. The application uses automatic module discovery, dependency injection, and a plugin-like system for extensibility.

## Application Architecture

### Core Principles

1. **Modular Monolith**: Self-contained modules that can be enabled/disabled via configuration
2. **Dependency Injection**: Centralized DI container for service management
3. **Auto-Discovery**: Modules are automatically discovered and loaded at startup
4. **Game-Agnostic Design**: Core game infrastructure supports multiple game types
5. **Clean Architecture**: Separation of concerns with domain, services, repositories, and API layers

## Directory Structure

```
game-service/app/
├── main.py                 # FastAPI application entry point
├── config.py              # Application configuration (Pydantic Settings)
├── consumer.py             # RabbitMQ consumer entry point (separate deployment)
│
├── core/                   # Core game infrastructure (shared across modules)
│   └── game/
│       ├── interfaces.py  # Game interfaces (GameInterface, GameStateInterface, etc.)
│       ├── registry.py    # GameRegistry for managing game implementations
│       ├── factory.py     # GameFactory for creating game instances
│       └── base.py        # BaseGame base class
│
├── modules/                # Feature modules (auto-discovered)
│   ├── games/             # Game engine and session management
│   ├── game_logger/       # Gameplay data logging and analytics
│   ├── ai_player/         # MCTS-based AI player
│   └── chatbot/           # RAG-based chatbot
│
└── shared/                # Shared utilities and infrastructure
    ├── container.py       # Dependency injection container
    ├── module_loader.py   # Automatic module discovery and loading
    ├── exceptions.py      # Custom exception classes
    └── messaging/         # RabbitMQ client utilities
```

## Module System

### Module Structure

Each module follows a consistent structure:

```
module_name/
├── __init__.py            # Module initialization (setup_module function)
├── api.py or api/         # FastAPI router and endpoints
├── service.py              # Business logic services
├── dto.py                  # Data Transfer Objects (Pydantic models)
├── models.py               # Domain models (if applicable)
├── types.py                # Type definitions (if applicable)
└── [module-specific]/      # Additional module-specific directories
```

### Module Initialization

Every module must implement a `setup_module()` function:

```python
def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    """
    Setup and configure the module.
    
    Returns:
        APIRouter if module is enabled, None otherwise
    """
    # 1. Check if module is enabled in config
    # 2. Register services in DI container
    # 3. Create and return FastAPI router
```

### Module Metadata

Modules expose metadata via module-level constants:

- `MODULE_NAME`: Human-readable module name
- `MODULE_DESCRIPTION`: Module description
- `MODULE_VERSION`: Module version

## Module Details

### 1. Games Module (`modules/games/`)

**Purpose**: Core game engine supporting multiple game types with stateless and stateful operations.

**Scope**:
- Game-agnostic game logic infrastructure
- Game session management with persistence
- Support for multiple game implementations (Connect Four, etc.)
- Stateless game operations (for AI/ML)
- Stateful session operations (for user gameplay)

**Structure**:
```
games/
├── domain/                 # Domain models and events
│   ├── models.py          # GameSession, GameSessionStatus
│   └── events.py          # GameStartedEvent, GameMoveResponseEvent, GameEndedEvent
│
├── games/                  # Game implementations
│   └── connect_four/
│       ├── game.py        # ConnectFourGame implementation
│       └── models.py      # ConnectFourState, ConnectFourMove
│
├── services/               # Business logic
│   ├── game_service.py    # Stateless game operations
│   └── session_service.py # Stateful session management
│
├── repositories/          # Persistence layer
│   ├── models.py          # GameSessionModel (ORM)
│   ├── mappers.py         # GameSessionMapper (domain ↔ ORM)
│   └── session_repository.py # GameSessionRepository
│
└── api/                   # REST API
    ├── router.py          # FastAPI routes
    └── dto.py             # Request/Response DTOs
```

**Key Components**:
- **GameService**: Stateless operations (create state, apply move, get legal moves)
- **GameSessionService**: Stateful operations with persistence (create session, apply move to session)
- **GameRegistry**: Manages registered game implementations
- **GameFactory**: Creates game instances by type

**API Endpoints**:
- `GET /api/v1/games/games` - List available games
- `POST /api/v1/games/games/create` - Create game state (stateless)
- `POST /api/v1/games/games/move` - Apply move (stateless)
- `POST /api/v1/games/sessions` - Create game session
- `GET /api/v1/games/sessions/{session_id}` - Get session
- `POST /api/v1/games/sessions/{session_id}/moves` - Apply move to session

**Configuration**:
- `MODULE_GAMES_ENABLED`: Enable/disable module (default: `True`)

---

### 2. Game Logger Module (`modules/game_logger/`)

**Purpose**: Logs gameplay data for analytics, ML training, and dataset generation.

**Scope**:
- Game-agnostic move logging
- Game session metadata tracking
- Dataset export to Parquet format
- DVC integration for dataset versioning
- RabbitMQ event consumption (asynchronous logging)

**Structure**:
```
game_logger/
├── repositories/          # Persistence layer
│   ├── models.py         # GameLogModel, GameSessionModel (ORM)
│   ├── mappers.py        # Mappers (domain ↔ ORM)
│   └── game_log_repository.py # Repository
│
├── consumers/            # RabbitMQ consumers
│   └── event_consumer.py # Listens to game events
│
├── service.py           # GameLoggerService
├── dataset_export.py    # Dataset export functionality
├── dvc_manager.py       # DVC versioning integration
├── api.py              # REST API endpoints
└── dto.py              # DTOs
```

**Key Components**:
- **GameLoggerService**: Core logging service
- **GameLogRepository**: Persistence for game logs
- **DatasetExportService**: Exports logs to Parquet for ML
- **DVCManager**: Manages dataset versioning with DVC
- **GameLoggerEventConsumer**: Consumes RabbitMQ events asynchronously

**API Endpoints**:
- `POST /api/v1/game-logger/log-move` - Log a move
- `POST /api/v1/game-logger/create-session` - Create session metadata
- `POST /api/v1/game-logger/finish-session` - Finish session
- `GET /api/v1/game-logger/game/{game_id}/logs` - Get game logs
- `POST /api/v1/game-logger/export-dataset` - Export dataset

**Configuration**:
- `MODULE_GAME_LOGGER_ENABLED`: Enable/disable module (default: `True`)
- `LOGGER_ENABLED`: Enable logging functionality (default: `True`)
- `DATASET_EXPORT_ENABLED`: Enable dataset export (default: `True`)
- `DVC_ENABLED`: Enable DVC versioning (default: `False`)

**Note**: The logger listens to RabbitMQ events asynchronously and does not block game operations.

---

### 3. AI Player Module (`modules/ai_player/`)

**Purpose**: MCTS-based AI player with multiple difficulty levels and dynamic difficulty adjustment.

**Scope**:
- MCTS algorithm implementation for game moves
- Multiple difficulty levels (LOW, MEDIUM, HIGH, VERY_HIGH)
- Dynamic difficulty adjustment based on player performance
- Integration with games module for game-agnostic AI

**Structure**:
```
ai_player/
├── service.py    # AIPlayerService
├── api.py        # REST API endpoints
├── dto.py        # DTOs
├── models.py     # AIPlayerLevel enum, MoveRequest, MoveResponse
└── types.py      # MoveEvaluation, DifficultyAdjustmentResult
```

**Key Components**:
- **AIPlayerService**: Core AI player logic with MCTS
- **AIPlayerLevel**: Difficulty level enumeration
- **Difficulty Adjustment**: Automatic difficulty tuning based on win rates

**API Endpoints**:
- `POST /api/v1/ai-player/move` - Get AI move
- `POST /api/v1/ai-player/adjust-difficulty` - Adjust difficulty

**Configuration**:
- `MODULE_AI_PLAYER_ENABLED`: Enable/disable module (default: `True`)
- `AI_PLAYER_MCTS_ITERATIONS_LOW`: Iterations for low difficulty (default: `100`)
- `AI_PLAYER_MCTS_ITERATIONS_MEDIUM`: Iterations for medium (default: `500`)
- `AI_PLAYER_MCTS_ITERATIONS_HIGH`: Iterations for high (default: `2000`)
- `AI_PLAYER_MCTS_ITERATIONS_VERY_HIGH`: Iterations for very high (default: `5000`)
- `AI_PLAYER_ENABLE_DYNAMIC_DIFFICULTY`: Enable auto-adjustment (default: `True`)

---

### 4. Chatbot Module (`modules/chatbot/`)

**Purpose**: RAG-based chatbot for game rules, platform guidance, and user assistance.

**Scope**:
- Retrieval-Augmented Generation (RAG) for context-aware responses
- Vector database integration (Chroma, Pinecone, etc.)
- LLM integration (OpenAI, Anthropic, etc.)
- Conversation history management
- Caching for performance

**Structure**:
```
chatbot/
├── service.py    # ChatbotService
├── api.py        # REST API endpoints
├── dto.py        # DTOs
├── models.py     # Chat models
└── types.py      # ChatResponse, ConversationHistory, etc.
```

**Key Components**:
- **ChatbotService**: Core chatbot logic with RAG
- **Vector DB Integration**: For semantic search
- **LLM Integration**: For response generation
- **Caching**: Response caching for performance

**API Endpoints**:
- `POST /api/v1/chatbot/chat` - Send chat message
- `GET /api/v1/chatbot/history` - Get conversation history

**Configuration**:
- `MODULE_CHATBOT_ENABLED`: Enable/disable module (default: `True`)
- `CHATBOT_LLM_PROVIDER`: LLM provider (default: `"openai"`)
- `CHATBOT_LLM_MODEL`: Model name (default: `"gpt-4"`)
- `CHATBOT_VECTOR_DB_TYPE`: Vector DB type (default: `"chroma"`)
- `CHATBOT_CACHE_ENABLED`: Enable caching (default: `True`)

---

## App Bundling System

### Module Loader (`shared/module_loader.py`)

The **ModuleLoader** is responsible for automatic module discovery and initialization.

#### Discovery Process

1. **Auto-Discovery**: Scans `app/modules/` directory for Python packages
2. **Module Detection**: Identifies modules by presence of `__init__.py` with `setup_module()` function
3. **Configuration Check**: Verifies module is enabled via `MODULE_{NAME}_ENABLED` setting
4. **Initialization**: Calls `setup_module()` for each enabled module
5. **Router Registration**: Registers returned routers with FastAPI app

#### Module Loading Flow

```
Application Start
    ↓
ModuleLoader.discover_modules()
    ↓
For each module:
    ├─ Check MODULE_{NAME}_ENABLED
    ├─ Import module
    ├─ Call setup_module()
    ├─ Register services in DI container
    └─ Register router with FastAPI
    ↓
Application Ready
```

#### Module Status

The loader tracks module status:
- `loaded`: Successfully loaded and registered
- `disabled`: Disabled in configuration
- `error`: Failed to load (with error details)

### Dependency Injection Container (`shared/container.py`)

The **Container** provides centralized dependency management.

#### Features

- **Singleton Support**: Services can be registered as singletons
- **Factory Functions**: Lazy instantiation via factory functions
- **Direct Instances**: Direct instance registration
- **Type-Based Lookup**: Services retrieved by type

#### Usage Pattern

```python
# In module setup_module():
from app.shared.container import container

# Register service
container.register(MyService, instance=MyService(), singleton=True)

# Or with factory
container.register(MyService, factory=lambda: MyService(deps), singleton=True)

# Retrieve service
service = container.get(MyService)
```

#### Service Registration

Modules register their services during `setup_module()`:
1. Check if service already registered (`container.has()`)
2. Register service with appropriate lifecycle (singleton/non-singleton)
3. Services are available to other modules via `container.get()`

### Application Initialization (`main.py`)

The FastAPI application is created via `create_app()`:

1. **Create FastAPI Instance**: With metadata from settings
2. **Register Exception Handlers**: Custom exception handling
3. **Add CORS Middleware**: Cross-origin resource sharing
4. **Load Modules**: Via `module_loader.load_all_modules()`
5. **Add Health Endpoints**: `/`, `/health`, `/modules`

### Module Router Registration

Routers are registered with automatic prefix generation:

- **Module Name**: `games` → Route prefix: `/api/v1/games`
- **Module Name**: `game_logger` → Route prefix: `/api/v1/game-logger`
- **Tags**: Auto-generated from module name (e.g., `"Games"`, `"Game Logger"`)

## Core Infrastructure

### Game Infrastructure (`core/game/`)

Shared infrastructure for game-agnostic design:

- **GameInterface**: Abstract interface for game implementations
- **GameStateInterface**: Interface for game state representations
- **GameMoveInterface**: Interface for move representations
- **GameRegistry**: Manages registered game types
- **GameFactory**: Creates game instances by type
- **BaseGame**: Base class for game implementations

This infrastructure allows the system to support multiple game types without game-specific code in core services.

## Configuration System

### Settings (`config.py`)

Uses **Pydantic Settings** for configuration management:

- **Environment Variables**: Loaded from `.env` file
- **Type Safety**: Typed configuration with defaults
- **Validation**: Automatic validation on load
- **Module Flags**: `MODULE_{NAME}_ENABLED` for each module

### Module Configuration Pattern

Each module checks its enabled status:

```python
if not getattr(settings, "MODULE_{NAME}_ENABLED", True):
    return None  # Module disabled
```

## Deployment Architecture

### Main Application (`main.py`)

- **Service**: FastAPI REST API
- **Port**: 8000 (configurable)
- **Endpoints**: All module APIs + health checks

### Consumer Service (`consumer.py`)

- **Service**: RabbitMQ message consumer
- **Purpose**: Asynchronous event processing
- **Modules**: Game logger event consumption
- **Deployment**: Separate container/process (optional)

### Docker Structure

- **Dockerfile**: Main application image
- **Dockerfile.consumer**: Consumer service image
- **docker-compose.yml**: Orchestration for development

## Adding a New Module

### Step 1: Create Module Directory

```
app/modules/my_module/
├── __init__.py
├── service.py
├── api.py
└── dto.py
```

### Step 2: Implement `setup_module()`

```python
# app/modules/my_module/__init__.py
from fastapi import APIRouter
from app.shared.container import container
from .service import MyService
from .api import create_router

MODULE_NAME = "my_module"
MODULE_DESCRIPTION = "My module description"
MODULE_VERSION = "1.0.0"

def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    from app.config import settings
    
    if not getattr(settings, "MODULE_MY_MODULE_ENABLED", True):
        return None
    
    # Register service
    if not container.has(MyService):
        container.register(MyService, instance=MyService(), singleton=True)
    
    return create_router(container.get(MyService))
```

### Step 3: Add Configuration

```python
# app/config.py
MODULE_MY_MODULE_ENABLED: bool = True
```

### Step 4: Module Auto-Discovery

The module will be automatically discovered and loaded on next application start.

## Module Dependencies

### Inter-Module Communication

Modules can depend on each other via the DI container:

```python
# Module A depends on Module B's service
from app.shared.container import container
from app.modules.module_b.service import ModuleBService

service_b = container.get(ModuleBService)  # If registered
```

### Shared Infrastructure

- **Core Game Infrastructure**: Available to all modules
- **DI Container**: Global container instance
- **Configuration**: Shared settings object
- **Messaging**: Shared RabbitMQ client

## Best Practices

1. **Module Independence**: Modules should be independently deployable (conceptually)
2. **Service Registration**: Always check `container.has()` before registering
3. **Error Handling**: Modules should handle their own errors gracefully
4. **Configuration**: Use settings for all configurable behavior
5. **Type Hints**: Use type hints throughout for better IDE support
6. **Documentation**: Document module purpose and API in module `__init__.py`

## Summary

The Game Service uses a **modular monolith architecture** with:

- **Automatic Module Discovery**: No manual registration needed
- **Dependency Injection**: Centralized service management
- **Game-Agnostic Design**: Supports multiple game types
- **Clean Architecture**: Separation of concerns
- **Configuration-Driven**: Enable/disable modules via settings
- **Extensible**: Easy to add new modules or game types

This architecture provides the benefits of microservices (modularity, independence) while maintaining the simplicity of a monolith (single deployment, shared infrastructure).










