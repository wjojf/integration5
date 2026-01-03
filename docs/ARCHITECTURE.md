# BanditGames Platform - Complete Architecture Documentation

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Overview](#system-overview)
3. [Core Architectural Principles](#core-architectural-principles)
4. [Project Descriptions](#project-descriptions)
   - [API Gateway](#api-gateway)
   - [Platform Backend](#platform-backend)
   - [Game Service](#game-service)
5. [Communication Patterns](#communication-patterns)
6. [Data Flow Examples](#data-flow-examples)
7. [Infrastructure](#infrastructure)
8. [Security Architecture](#security-architecture)
9. [Deployment](#deployment)
10. [RabbitMQ Event Contracts](#rabbitmq-event-contracts)
11. [External Game Integration](#external-game-integration)

---

## Executive Summary

The BanditGames platform is a microservices-based gaming platform consisting of three main services:

1. **API Gateway** (Spring Cloud Gateway) - Centralized entry point for all client requests
2. **Platform Backend** (Spring Boot) - Handles platform features (lobbies, friends, achievements, game registry)
3. **Game Service** (Python/FastAPI) - Handles all game-related logic (game state, sessions, AI, ML, chatbot)

**Key Architectural Decisions:**
- **Strict Service Separation**: Platform Backend and Game Service **NEVER directly call each other**
- **Communication**: REST via Gateway (synchronous) + RabbitMQ Events (asynchronous)
- **ACL Pattern**: External games integrated via API Gateway transformation
- **Event-Driven**: All service-to-service operations use RabbitMQ events

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL CLIENTS                                   │
│  ┌──────────────┐  ┌──────────────┐                                         │
│  │   Web App    │  │  Mobile App  │                                         │
│  │   (React)    │  │              │                                         │
│  └──────┬───────┘  └──────┬───────┘                                         │
│         │                 │                                                  │
│         └─────────────────┘                                                  │
│                            │                                                 │
│                            │ HTTPS/REST                                      │
│                            │ WebSocket                                       │
│                            │ JWT Token                                       │
└────────────────────────────┼─────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Spring Cloud Gateway)                   │
│                         Port: 8080                                           │
│  • JWT Authentication (Keycloak)                                             │
│  • Rate Limiting (Redis)                                                     │
│  • Circuit Breaker                                                           │
│  • Request Routing                                                           │
│  • ACL Transformation (external games)                                       │
└────────────┬───────────────────────────────┬───────────────────────────────┘
             │                               │
             │ REST                           │ REST
             │ (with JWT)                     │ (with API Key)
             │                                │
             ▼                                ▼
┌──────────────────────────┐    ┌──────────────────────────────────────────┐
│  PLATFORM BACKEND        │    │         GAME SERVICE                     │
│  (Spring Boot)           │    │         (Python/FastAPI)                 │
│  Port: 8081              │    │         Port: 8000                       │
│                          │    │                                          │
│  • Lobby Management      │    │  • Game Engine (Connect Four)            │
│  • Friends System        │    │  • Game Sessions                         │
│  • Achievements          │    │  • Game State                            │
│  • Game Registry         │    │  • AI Player (MCTS)                      │
│  • ACL Adapters          │    │  • ML Models                              │
│  • WebSocket Support     │    │  • Chatbot (RAG)                          │
│                          │    │  • Game Logger                            │
│  ❌ NO Direct Calls      │    │  ❌ NO Direct Calls                       │
│     to Game Service      │    │     to Platform Backend                   │
└──────────┬───────────────┘    └───────────┬───────────────────────────────┘
           │                                │
           │                                │
           │ RabbitMQ Events (Async)        │ RabbitMQ Events (Async)
           │ ONLY Communication Method     │ ONLY Communication Method
           │                                │
           └────────────┬───────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RABBITMQ MESSAGE BROKER                             │
│                         Ports: 5672 (AMQP), 15672 (Management)              │
│                                                                              │
│  Exchanges:                                                                  │
│  • game_events (topic) - Internal game events                                │
│  • gameExchange (topic) - External game events (chess)                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Architectural Principles

1. **Strict Service Separation**: Platform Backend and Game Service **NEVER directly call each other**
2. **Communication Patterns**:
   - **REST**: Frontend → API Gateway → Service (synchronous queries only)
   - **Events**: Service ↔ RabbitMQ ↔ Service (asynchronous operations)
   - **ACL**: API Gateway transforms external game formats
3. **Game Service Owns All Native Game State**: Connect Four game logic, sessions, moves
4. **Platform Backend Owns Platform Features**: Lobbies, friends, achievements, game registry
5. **External Games**: Managed by external services, integrated via ACL

---

## Project Descriptions

### API Gateway

**Technology**: Spring Cloud Gateway 3.x, Java 21  
**Port**: 8080  
**Repository**: `api-gateway/`

#### Responsibilities

- ✅ **JWT Authentication**: Validates tokens from Keycloak
- ✅ **Rate Limiting**: Per-user and per-endpoint rate limiting using Redis
- ✅ **Request Routing**: Routes requests to appropriate backend services
- ✅ **Circuit Breaking**: Resilience4j circuit breakers for fault tolerance
- ✅ **ACL Transformation**: Transforms external game formats to platform format
- ✅ **Service-to-Service Auth**: API key-based authentication for internal services
- ✅ **Request/Response Logging**: Comprehensive logging for monitoring
- ❌ **No Business Logic**: Pure routing and transformation layer

#### Key Components

**Filters:**
- `AuthenticationFilter`: Validates JWT tokens from Keycloak
- `ACLTransformFilter`: Transforms external game requests/responses (bi-directional)
- `GenerateGameIdFilter`: Generates UUID for external game registration
- `RateLimitFilter`: Redis-based rate limiting
- `CircuitBreakerFilter`: Resilience4j circuit breakers
- `ServiceToServiceAuthFilter`: Validates API keys for service-to-service calls

**Routes:**
- `/api/platform/**` → `platform-backend:8081`
- `/api/games/**` → `game-service:8000`
- `/api/external/chess/**` → External chess service (with ACL transformation)

#### Configuration

**Environment Variables:**
```bash
PLATFORM_BACKEND_URL=http://platform-backend:8081
GAME_SERVICE_URL=http://game-service:8000
CHESS_GAME_SERVICE_URL=http://localhost:8080
KEYCLOAK_ISSUER_URI=http://keycloak:8090/realms/banditgames
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Key Features:**
- Bi-directional ACL transformation for external games
- Automatic gameId generation for external game registration
- Circuit breaker with fallback endpoints
- Aggregated Swagger/OpenAPI documentation

---

### Platform Backend

**Technology**: Spring Boot 3.x, Java 21, Spring Modulith  
**Port**: 8081  
**Repository**: `platform-backend/`

#### Architecture

The application follows **Hexagonal Architecture** (Ports and Adapters) with **Spring Modulith** for modular structure:

- **Domain Layer**: Core business entities and domain logic
- **Adapter Layer**: External integrations (persistence, APIs, messaging)

#### Modules

**1. Lobby Module**
- Creating and joining game lobbies
- Lobby status management (WAITING, READY, STARTED, CANCELLED, COMPLETED)
- WebSocket support for real-time lobby updates
- Event-driven game session start flow

**2. Friends Module**
- Friend request system
- Friendship status management (PENDING, ACCEPTED, REJECTED, BLOCKED)
- Friend search and recommendations

**3. Chat Module**
- Direct messaging between users
- Message status tracking (SENT, DELIVERED, READ)
- WebSocket support for real-time messaging

**4. Achievements Module**
- Game achievements definition
- User achievement tracking with unlock dates
- Achievement unlock events from game events

**5. Games Module (Registry)**
- Game registration and metadata
- User favorite games management
- Game discovery and search

**6. ACL Module (Anti-Corruption Layer)**
- External game event adapters (Chess)
- Event transformation from external format to platform format
- NO direct calls to game-service

**7. Games Adapter Module**
- `GameSessionRequestPublisher`: Publishes `game.session.start.requested` events
- `GameSessionStartedConsumer`: Consumes `game.session.started` events

#### Key Components

**Event Publishers:**
- `GameSessionRequestPublisher`: Publishes session start requests when lobby starts

**Event Consumers:**
- `GameSessionStartedConsumer`: Consumes session started events and updates lobby
- `ChessGameACLAdapter`: Consumes external chess events and transforms them
- `AchievementEventConsumer`: Consumes game events for achievement unlocks

**Controllers:**
- `LobbyController`: Lobby management endpoints
- `ChessGameController`: External chess game registration (metadata only)

#### Database

**PostgreSQL** (`banditgames` database):
- `lobbies`, `lobby_players`, `lobby_invites`
- `friendships`
- `messages`
- `achievements`, `user_achievements`
- `games`, `user_favorite_games`

#### Communication Rules

✅ **ALLOWED**:
- REST via API Gateway (from frontend)
- RabbitMQ events (to/from game-service)
- WebSocket (to frontend)

❌ **FORBIDDEN**:
- Direct REST calls to game-service
- Direct database access to game-service database

---

### Game Service

**Technology**: Python 3.11+, FastAPI, PostgreSQL  
**Port**: 8000  
**Repository**: `game-service/`

#### Architecture

Clean Architecture with modular design:
- **Core Layer**: Game interfaces and abstractions
- **Modules Layer**: Feature modules (games, AI, ML, chatbot, logger)
- **Shared Layer**: Common utilities (messaging, DI container)

#### Modules

**1. Games Module**
- **Game Engine**: Connect Four implementation
- **Session Management**: Full lifecycle (create, update, end)
- **Game State**: Current state of all active games
- **Move Processing**: Validate and apply moves
- **Event Publishing**: Publishes `game.move.applied`, `game.session.ended` events

**2. AI Player Module**
- MCTS (Monte Carlo Tree Search) implementation
- AI opponent for Connect Four
- Configurable difficulty levels

**3. ML Models Module**
- Policy model for move prediction
- Win probability model
- Model training and evaluation
- Dataset generation

**4. Chatbot Module**
- RAG (Retrieval-Augmented Generation) based chatbot
- Game rules assistance
- Platform information queries

**5. Game Logger Module**
- Detailed gameplay logging
- Move-by-move game state tracking
- Analytics and statistics

**6. Event Consumers**
- `GameSessionEventConsumer`: Consumes `game.session.start.requested` events
- `GameLoggerEventConsumer`: Consumes game events for logging

#### Key Components

**Services:**
- `GameSessionService`: Manages game sessions, applies moves, publishes events
- `GameService`: Core game engine operations
- `AIPlayerService`: AI opponent logic
- `ChatbotService`: RAG-based chatbot
- `GameLoggerService`: Gameplay logging

**Event Publisher:**
- `events.py`: Event publisher factory for RabbitMQ

**Consumers:**
- `session_consumer.py`: Handles session start requests
- `event_consumer.py`: Handles game events for logging

#### Database

**PostgreSQL** (`game_service` database):
- `game_sessions`: Active and completed game sessions
- `game_logs`: Detailed move-by-move game logs
- `ml_datasets`: Training datasets
- `ml_models`: Model metadata

#### Communication Rules

✅ **ALLOWED**:
- REST via API Gateway (from frontend)
- RabbitMQ events (to/from platform-backend)

❌ **FORBIDDEN**:
- Direct REST calls to platform-backend
- Direct database access to platform-backend database

---

## Communication Patterns

### Pattern 1: Synchronous REST (Frontend → Services)

```
Frontend → API Gateway → Service → Response
```

**Use Cases:**
- User authentication
- Lobby operations
- Game queries
- Chatbot queries
- Chess game REST operations

### Pattern 2: Asynchronous Events (RabbitMQ)

```
Producer Service → RabbitMQ → Consumer Service
```

**Use Cases:**
- Game move events
- Game session events
- Achievement unlocks
- External game events (chess)
- Game logging

### Pattern 3: WebSocket (Real-time Updates)

```
Frontend ↔ API Gateway ↔ Platform Backend
```

**Use Cases:**
- Lobby updates
- Game state updates
- Real-time notifications

---

## Data Flow Examples

### Connect Four: Start Game from Lobby

```
1. Frontend → Gateway → Platform Backend: POST /api/platform/lobbies/{id}/start
2. Platform Backend: Updates lobby status to STARTED
3. Platform Backend → RabbitMQ: game.session.start.requested
4. Game Service ← RabbitMQ: Creates Connect Four session
5. Game Service → RabbitMQ: game.session.started
6. Platform Backend ← RabbitMQ: Updates lobby with session_id
7. Platform Backend → WebSocket: Notifies frontend
```

### Connect Four: Make Move

```
1. Frontend → Gateway → Game Service: POST /api/games/sessions/{id}/moves
2. Game Service: Validates and applies move
3. Game Service → PostgreSQL: Updates session state
4. Game Service → RabbitMQ: game.move.applied
5. Platform Backend ← RabbitMQ: Checks achievements
6. Game Logger ← RabbitMQ: Logs move
7. Game Service → Frontend: Returns updated game state
```

### Chess: Register External Game

```
1. Frontend → Gateway: POST /api/external/chess/register
2. Gateway: Generates gameId (UUID)
3. Gateway: ACL transforms to external format
4. Gateway → External Chess Service: POST /api/platform/register/{gameId}
5. External Chess Service → RabbitMQ: game.registered
6. Platform Backend ← RabbitMQ: Stores metadata
```

### Chess: Make Move (External Game)

```
1. Frontend → Gateway: POST /api/external/chess/moves/{gameId}
2. Gateway: ACL transforms to external format
3. Gateway → External Chess Service: POST /api/moves/{gameId}
4. External Chess Service: Validates and applies move
5. External Chess Service → RabbitMQ: move.made
6. Platform Backend ← RabbitMQ: Transforms and republishes
7. Game Logger ← RabbitMQ: Logs move (doesn't manage state)
```

---

## Infrastructure

### Services

| Service | Technology | Port | Purpose |
|---------|-----------|------|---------|
| **API Gateway** | Spring Cloud Gateway | 8080 | Routing, Auth, Rate Limiting |
| **Platform Backend** | Spring Boot | 8081 | Platform features |
| **Game Service** | FastAPI | 8000 | Game logic, AI, ML |
| **Keycloak** | Keycloak | 8090 | SSO authentication |
| **RabbitMQ** | RabbitMQ | 5672 | Message broker |
| **Redis** | Redis | 6379 | Rate limiting, cache |
| **PostgreSQL (Platform)** | PostgreSQL | 5432 | Platform database |
| **PostgreSQL (Game)** | PostgreSQL | 5432 | Game service database |
| **PostgreSQL (Keycloak)** | PostgreSQL | 5432 | Keycloak database |

### Network

All services run in Docker network `banditgames-network` (bridge mode).

---

## Security Architecture

### Authentication Flow

```
1. Client → Keycloak: Login
2. Keycloak → Client: JWT Token
3. Client → API Gateway: Request with JWT
4. API Gateway: Validates JWT with Keycloak
5. API Gateway → Service: Request with user context (X-User-Id, X-Username)
6. Service: Uses user context for authorization
```

### Service-to-Service Authentication

- API Gateway adds `X-API-Key` header for internal service calls
- Services validate API key for service-to-service requests

### Rate Limiting

- Redis-based rate limiting in API Gateway
- Per-user and per-endpoint limits
- Configurable limits per route

---

## Deployment

### Docker Compose

All services are defined in `docker-compose.yml`:

```yaml
services:
  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
  
  platform-backend:
    build: ./platform-backend
    ports:
      - "8081:8081"
  
  game-service:
    build: ./game-service
    ports:
      - "8000:8000"
  
  # ... infrastructure services
```

### Running

```bash
docker-compose up -d
```

---

## RabbitMQ Event Contracts

### Internal Events (game_events exchange)

#### game.session.start.requested
- **Publisher**: Platform Backend
- **Consumer**: Game Service
- **Purpose**: Request game service to create a session

**Message Format:**
```json
{
  "lobby_id": "lobby-uuid",
  "session_id": "session-uuid",
  "game_type": "connect_four",
  "game_id": "game-uuid",
  "player_ids": ["player-uuid-1", "player-uuid-2"],
  "starting_player_id": "player-uuid-1",
  "configuration": { "rows": 6, "columns": 7 }
}
```

#### game.session.started
- **Publisher**: Game Service (native games) or Platform Backend ACL (external games)
- **Consumer**: Platform Backend, Game Logger
- **Purpose**: Notify that a session has started

**Message Format:**
```json
{
  "eventId": "event-uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "lobby_id": "lobby-uuid",
  "session_id": "session-uuid",
  "game_id": "game-uuid",
  "game_type": "connect_four",
  "status": "active",
  "game_state": { ... },
  "player_ids": ["player-uuid-1", "player-uuid-2"]
}
```

#### game.move.applied
- **Publisher**: Game Service (native games) or Platform Backend ACL (external games)
- **Consumer**: Platform Backend, Game Logger
- **Purpose**: Notify that a move has been applied

**Message Format:**
```json
{
  "eventId": "event-uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "session_id": "session-uuid",
  "game_id": "game-uuid",
  "game_type": "connect_four",
  "player_id": "player-uuid-1",
  "move": { "column": 3 },
  "game_state": { ... },
  "status": "active" | "finished",
  "winner_id": null | "player-uuid-1"
}
```

#### game.session.ended
- **Publisher**: Game Service (native games) or Platform Backend ACL (external games)
- **Consumer**: Platform Backend, Game Logger
- **Purpose**: Notify that a session has ended

**Message Format:**
```json
{
  "eventId": "event-uuid",
  "timestamp": "2024-01-01T12:00:00Z",
  "session_id": "session-uuid",
  "game_id": "game-uuid",
  "game_type": "connect_four",
  "status": "finished",
  "winner_id": "player-uuid-1" | null,
  "final_game_state": { ... },
  "total_moves": 42
}
```

### External Events (gameExchange)

#### game.created, move.made, game.ended (Chess)
- **Publisher**: External Chess Service
- **Consumer**: Platform Backend ACL Adapter
- **Purpose**: External game events that need transformation

See `docs/chess.md` for external chess service event formats.

---

## External Game Integration

### Adding a New External Game

**Step 1: Add API Gateway Routes**

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: external-newgame-register
          uri: ${NEWGAME_SERVICE_URL:http://newgame-service:8080}
          predicates:
            - Path=/api/external/newgame/register
          filters:
            - name: GenerateGameId
            - name: ACLTransform
              args:
                gameType: newgame
                direction: both
```

**Step 2: Extend ACL Transform Filter**

Add transformation logic in `ACLTransformFilter` for the new game type.

**Step 3: Add Event Adapter (if needed)**

```java
@Component
public class NewGameACLAdapter {
    @RabbitListener(queues = "newgame.events")
    public void handleNewGameEvent(NewGameEvent event) {
        // Transform and republish to game_events exchange
        // NO direct calls to game-service
    }
}
```

### Key Principles

- External games manage their own state
- API Gateway handles REST transformation (ACL)
- Platform Backend handles event transformation (ACL)
- Game Service only logs external game events (doesn't manage state)
- NO direct calls between services

---

## Summary

**Architecture:**
- ✅ API Gateway: Routing, authentication, ACL transformation
- ✅ Platform Backend: Platform features, event coordination
- ✅ Game Service: Native game logic, state, AI, ML
- ✅ External Games: Integrated via ACL in Gateway

**Communication:**
- ✅ Frontend ↔ Services: Via API Gateway (REST)
- ✅ Service ↔ Service: Via RabbitMQ (Events)
- ❌ Service ↔ Service: NO direct REST calls

**Key Features:**
- Strict service separation
- Event-driven architecture
- Generic external game integration
- Scalable and maintainable design
