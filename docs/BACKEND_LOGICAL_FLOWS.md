# Backend Logical Flows - Complete System Documentation

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Game Start Flow](#game-start-flow)
3. [Game Continuation Flow](#game-continuation-flow)
4. [Game End Flow](#game-end-flow)
5. [Event-Driven Communication Patterns](#event-driven-communication-patterns)
6. [Event Handlers and Consumers](#event-handlers-and-consumers)
7. [API Endpoints and Routing](#api-endpoints-and-routing)
8. [Database Interactions](#database-interactions)
9. [External Service Integration (Chess Backend)](#external-service-integration-chess-backend)
10. [Anti-Corruption Layer (ACL)](#anti-corruption-layer-acl)
11. [WebSocket Real-time Updates](#websocket-real-time-updates)
12. [Rate Limiting and Circuit Breaking](#rate-limiting-and-circuit-breaking)

---

## System Architecture Overview

### Core Services

The system consists of several microservices communicating via REST APIs and RabbitMQ:

1. **API Gateway** (Spring Cloud Gateway)
   - Single entry point for all client requests
   - Handles authentication (JWT via Keycloak)
   - Rate limiting (Redis-based)
   - Circuit breaking (Resilience4j)
   - CORS configuration
   - Route forwarding to backend services

2. **Platform Backend** (Spring Boot)
   - Lobby management
   - Player management
   - Achievement system
   - Event orchestration
   - ACL adapter for external chess service

3. **Game Service** (FastAPI/Python)
   - Game session management
   - Game logic (Connect Four, etc.)
   - Game logger (ML training data)
   - WebSocket server for real-time updates
   - AI player service
   - Chatbot service

4. **Chess Backend** (Spring Boot - External)
   - Chess game logic
   - Chess-specific state management
   - Publishes events to RabbitMQ

5. **RabbitMQ** (Message Broker)
   - Topic exchange: `game_events` (platform format)
   - Topic exchange: `gameExchange` (chess format)
   - Event-driven communication between services

6. **Redis**
   - Distributed rate limiting
   - Session state (if needed)

7. **PostgreSQL**
   - Platform Backend: Lobbies, players, achievements
   - Game Service: Game sessions, game logs (ML training data)

### Communication Patterns

- **Synchronous**: REST API calls (via API Gateway)
- **Asynchronous**: RabbitMQ events (event-driven architecture)
- **Real-time**: WebSocket connections (for game state updates)

---

## Game Start Flow

### Overview

The game start flow begins when a player creates a lobby and starts a game. The flow differs slightly between internal games (Connect Four) and external games (Chess).

### Internal Games (Connect Four) - Complete Flow

```
1. Frontend → API Gateway → Platform Backend
   POST /api/platform/lobbies/{lobbyId}/start
   { gameId: <connect-four-game-uuid> }
   
2. Platform Backend: StartLobbyService.startLobby()
   - Validates host can start lobby
   - Sets lobby.gameId = gameId
   - Sets lobby.status = STARTED
   - Publishes LobbyStartedEvent
   
3. Platform Backend: GameSessionRequestPublisher (EventListener)
   - Listens to LobbyStartedEvent
   - Publishes game.session.start.requested to RabbitMQ
   {
     session_id: <uuid>,
     game_id: <game-id>,
     game_type: "connect_four",
     lobby_id: <lobby-id>,
     player_ids: [<player1-id>, <player2-id>],
     starting_player_id: <player1-id>,
     configuration: {}
   }
   
4. RabbitMQ → Game Service: GameSessionEventConsumer
   - Consumes game.session.start.requested
   - Calls GameSessionService.create_session()
   - Creates game state using GameFactory
   - Saves session to database
   - Publishes game.session.started event
   
5. RabbitMQ → Platform Backend: GameSessionStartedConsumer
   - Consumes game.session.started
   - Updates lobby.sessionId = sessionId
   - Sets lobby.status = IN_PROGRESS
   - Saves lobby
   
6. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.session.started
   - Creates session log entry in database
   - Logs: session_id, game_type, player types, start_time
   
7. RabbitMQ → WebSocket Broadcaster: GameWebSocketEventConsumer
   - Consumes game.session.started
   - Broadcasts to all WebSocket connections for that session
   
8. Frontend receives WebSocket event
   - Updates UI to show game is active
   - Enables move input
```

### External Games (Chess) - Complete Flow

```
1. Frontend → API Gateway → Platform Backend
   POST /api/platform/lobbies/{lobbyId}/start
   { gameId: <chess-game-uuid> }
   
2. Platform Backend: StartLobbyService.startLobby()
   - Validates host can start lobby
   - Sets lobby.gameId = gameId
   - Sets lobby.status = STARTED
   - Publishes LobbyStartedEvent
   
3. Platform Backend: ChessGameLobbyHandler.onLobbyStarted()
   - Listens to LobbyStartedEvent
   - Checks if chess game was preregistered (when 2nd player joined)
   - If not preregistered, generates chessGameId (UUID)
   - Registers chess game with platform (POST /api/external/chess/register/{gameId})
   - Creates/activates chess game in chess backend (POST /api/games/{chessGameId})
     {
       whitePlayerId: <player1-id>,
       whitePlayerName: <player1-name>,
       blackPlayerId: <player2-id>,
       blackPlayerName: <player2-name>
     }
   - Stores external game instance mapping (lobbyId → chessGameId)
   - Publishes game.session.start.requested to RabbitMQ
   
4. RabbitMQ → Game Service: GameSessionEventConsumer
   - Consumes game.session.start.requested
   - Calls GameSessionService.create_session()
   - For chess, creates minimal state (no game factory)
   - Saves session to database
   - Publishes game.session.started event
   
5. Chess Backend: Creates chess game
   - Receives POST /api/games/{chessGameId}
   - Creates chess game instance
   - Publishes game.created event to RabbitMQ (gameExchange)
   
6. RabbitMQ → Platform Backend: ChessGameACLAdapter.handleGameCreated()
   - Consumes game.created from gameExchange
   - Looks up player IDs by name
   - Transforms to platform format
   - Republishes as game.session.started to game_events exchange
   
7. RabbitMQ → Platform Backend: GameSessionStartedConsumer
   - Consumes game.session.started
   - Updates lobby.sessionId = sessionId
   - Sets lobby.status = IN_PROGRESS
   
8. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.session.started
   - Creates session log entry
   
9. Frontend polls for external game instance
   - GET /api/platform/lobbies/{lobbyId}/external-game-instance
   - Receives chessGameId
   - Redirects to chess frontend: /game/{chessGameId}
```

### Key Components

#### StartLobbyService (Platform Backend)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/lobby/usecase/StartLobbyService.java`
- **Responsibilities**:
  - Validates host can start lobby
  - Sets game ID and status
  - Publishes `LobbyStartedEvent`

#### ChessGameLobbyHandler (Platform Backend)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/messaging/ChessGameLobbyHandler.java`
- **Responsibilities**:
  - Handles chess-specific lobby start logic
  - Preregisters players when lobby becomes full (2 players)
  - Creates chess game in external backend
  - Publishes session start request

#### GameSessionEventConsumer (Game Service)
- **Location**: `game-service/app/modules/games/consumers/session_consumer.py`
- **Responsibilities**:
  - Consumes `game.session.start.requested` events
  - Creates game session using `GameSessionService`
  - Publishes `game.session.started` event

#### GameSessionService (Game Service)
- **Location**: `game-service/app/modules/games/services/session_service.py`
- **Responsibilities**:
  - Creates game sessions (internal and external)
  - For chess: creates minimal state (external service owns state)
  - For Connect Four: creates full game state using `GameFactory`
  - Saves session to database

---

## Game Continuation Flow

### Overview

The game continuation flow handles player moves, state updates, and real-time synchronization across all connected clients.

### Internal Games (Connect Four) - Move Flow

```
1. Frontend → API Gateway → Game Service
   POST /api/v1/games/sessions/{sessionId}/moves
   {
     player_id: <player-id>,
     move_data: { column: 3 }
   }
   
2. Game Service: GameSessionService.apply_move()
   - Validates session exists and is active
   - Validates it's the player's turn
   - Uses GameFactory to get game instance
   - Applies move to game state
   - Updates session.game_state
   - Increments session.total_moves
   - Updates session.current_player_id
   - Checks if game is finished
   - If finished, sets session.status = FINISHED
   - Saves session to database
   - Publishes game.move.applied event
   - If finished, publishes game.session.ended event
   
3. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.move.applied
   - Extracts move data and game state
   - Calls GameLoggerService.log_connect_four_move()
   - Logs: session_id, move_number, player_id, board_state, etc.
   
4. RabbitMQ → WebSocket Broadcaster: GameWebSocketEventConsumer
   - Consumes game.move.applied
   - Broadcasts to all WebSocket connections for that session
   
5. Frontend receives WebSocket event
   - Updates game board UI
   - Updates current player indicator
   - Shows game status (ongoing/finished)
```

### External Games (Chess) - Move Flow

```
1. Chess Frontend → API Gateway → Chess Backend
   POST /api/moves/{chessGameId}
   {
     fromSquare: "e2",
     toSquare: "e4",
     player: "WHITE"
   }
   
2. Chess Backend: Validates and applies move
   - Validates move is legal
   - Updates chess game state
   - Publishes move.made event to RabbitMQ (gameExchange)
   {
     gameId: <chess-game-id>,
     fromSquare: "e2",
     toSquare: "e4",
     sanNotation: "e4",
     fenAfterMove: "...",
     player: "WHITE",
     moveNumber: 1,
     whitePlayer: <name>,
     blackPlayer: <name>
   }
   
3. RabbitMQ → Platform Backend: ChessGameACLAdapter.handleMoveMade()
   - Consumes move.made from gameExchange
   - Looks up player ID by name/color
   - Transforms to platform format
   - Republishes as game.move.applied to game_events exchange
   {
     game_id: <chess-game-id>,
     session_id: <chess-game-id>,  # Same as game_id for chess
     game_type: "chess",
     player_id: <platform-player-id>,
     move: {
       fromSquare: "e2",
       toSquare: "e4",
       sanNotation: "e4",
       fenAfterMove: "...",
       player: "WHITE",
       moveNumber: 1
     },
     newGameState: {
       fen: "...",
       move_number: 1,
       current_player: "BLACK"
     },
     gameStatus: "ongoing",
     type: "GAME_MOVE_APPLIED"
   }
   
4. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.move.applied
   - Calls GameLoggerService.log_chess_move()
   - Logs: session_id, move_number, player_id, move_data, game_state
   
5. RabbitMQ → WebSocket Broadcaster: GameWebSocketEventConsumer
   - Consumes game.move.applied
   - Broadcasts to all WebSocket connections for that session
   
6. RabbitMQ → Achievement System: GameEventConsumer
   - Consumes game.move.applied
   - Evaluates move-based achievements
   - Awards achievements if criteria met
   
7. Frontend receives WebSocket event
   - Updates chess board UI
   - Updates move history
   - Shows current player indicator
```

### Key Components

#### GameSessionService.apply_move() (Game Service)
- **Location**: `game-service/app/modules/games/services/session_service.py`
- **Responsibilities**:
  - Validates move (session active, correct player turn)
  - Applies move using game factory
  - Updates session state
  - Checks for game completion
  - Publishes events

#### ChessGameACLAdapter.handleMoveMade() (Platform Backend)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/ChessGameACLAdapter.java`
- **Responsibilities**:
  - Consumes chess move events from external service
  - Transforms to platform format
  - Looks up player IDs
  - Republishes for logging and notifications

#### GameLoggerService (Game Service)
- **Location**: `game-service/app/modules/game_logger/service.py`
- **Methods**:
  - `log_chess_move()`: Simplified logging for chess
  - `log_connect_four_move()`: Full ML training data for Connect Four
- **Responsibilities**:
  - Logs moves to database for ML training
  - Extracts board state, legal moves, etc.

---

## Game End Flow

### Overview

The game end flow handles game completion, winner determination, session cleanup, and achievement evaluation.

### Internal Games (Connect Four) - End Flow

```
1. Game Service: GameSessionService.apply_move()
   - After applying move, checks game status
   - If status != "ongoing", game is finished
   - Sets session.status = FINISHED
   - Sets session.winner_id = winner
   - Sets session.ended_at = now
   - Publishes game.session.ended event
   
2. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.session.ended
   - Extracts winner_id and player_ids
   - Determines winner: 'p1', 'p2', or 'draw'
   - Calls GameLoggerService.finish_session()
   - Updates session log: winner, num_moves, end_time
   
3. RabbitMQ → Platform Backend: GameSessionEndedConsumer
   - Consumes game.session.ended (from lobby queue)
   - Finds lobby by sessionId
   - Publishes GameEndedDomainEvent
   
4. Platform Backend: GameEventListener
   - Listens to GameEndedDomainEvent
   - Updates lobby.status = COMPLETED
   - Clears lobby.sessionId (optional)
   - Saves lobby
   
5. RabbitMQ → Achievement System: GameEventConsumer
   - Consumes game.session.ended (from achievements queue)
   - Evaluates game-end achievements
   - Awards achievements to players
   
6. RabbitMQ → WebSocket Broadcaster: GameWebSocketEventConsumer
   - Consumes game.session.ended
   - Broadcasts to all WebSocket connections
   
7. Frontend receives WebSocket event
   - Shows game over screen
   - Displays winner
   - Disables move input
```

### External Games (Chess) - End Flow

```
1. Chess Backend: Game ends (checkmate, draw, etc.)
   - Publishes game.ended event to RabbitMQ (gameExchange)
   {
     gameId: <chess-game-id>,
     winner: "WHITE" | "BLACK" | "DRAW",
     endReason: "CHECKMATE" | "DRAW" | "STALEMATE",
     finalFen: "...",
     totalMoves: 42,
     whitePlayer: <name>,
     blackPlayer: <name>
   }
   
2. RabbitMQ → Platform Backend: ChessGameACLAdapter.handleGameEnded()
   - Consumes game.ended from gameExchange
   - Looks up player IDs by name
   - Determines winner_id (maps WHITE/BLACK to platform player IDs)
   - Transforms to platform format
   - Republishes as game.session.ended to game_events exchange
   {
     game_id: <chess-game-id>,
     session_id: <chess-game-id>,
     game_type: "chess",
     winner_id: <platform-player-id>,
     gameResult: "WIN" | "DRAW",
     finalGameState: {
       fen: "...",
       totalMoves: 42,
       endReason: "CHECKMATE",
       winner: "WHITE"
     },
     type: "GAME_SESSION_ENDED"
   }
   
3. RabbitMQ → Game Logger: GameLoggerEventConsumer
   - Consumes game.session.ended
   - Extracts winner_id and player_ids
   - Determines winner: 'p1', 'p2', or 'draw'
   - Calls GameLoggerService.finish_session()
   - Updates session log
   
4. RabbitMQ → Platform Backend: GameSessionEndedConsumer
   - Consumes game.session.ended (from lobby queue)
   - Finds lobby by sessionId
   - Publishes GameEndedDomainEvent
   
5. Platform Backend: GameEventListener
   - Updates lobby.status = COMPLETED
   - Clears lobby.sessionId
   
6. RabbitMQ → Achievement System: GameEventConsumer
   - Evaluates achievements
   - Awards achievements
   
7. RabbitMQ → WebSocket Broadcaster: GameWebSocketEventConsumer
   - Broadcasts game end event
   
8. Frontend receives event
   - Shows game over screen
```

### Key Components

#### GameSessionService.apply_move() - Game Completion
- **Location**: `game-service/app/modules/games/services/session_service.py`
- **Responsibilities**:
  - Checks game status after move
  - Marks session as finished
  - Publishes `game.session.ended` event

#### GameLoggerService.finish_session() (Game Service)
- **Location**: `game-service/app/modules/game_logger/service.py`
- **Responsibilities**:
  - Updates session log with final results
  - Sets winner, num_moves, end_time

#### GameSessionEndedConsumer (Platform Backend)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/games/adapter/messaging/GameSessionEndedConsumer.java`
- **Responsibilities**:
  - Consumes `game.session.ended` events
  - Finds lobby by sessionId
  - Publishes `GameEndedDomainEvent` for lobby cleanup

#### ChessGameACLAdapter.handleGameEnded() (Platform Backend)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/ChessGameACLAdapter.java`
- **Responsibilities**:
  - Consumes chess game end events
  - Transforms to platform format
  - Republishes for logging and notifications

---

## Event-Driven Communication Patterns

### RabbitMQ Exchange Architecture

#### Platform Exchange: `game_events` (Topic Exchange)
- **Type**: Topic
- **Durable**: Yes
- **Routing Keys**:
  - `game.session.start.requested`
  - `game.session.started`
  - `game.move.applied`
  - `game.session.ended`
  - `game.achievement.unlocked`

#### Chess Exchange: `gameExchange` (Topic Exchange)
- **Type**: Topic
- **Durable**: Yes
- **Routing Keys**:
  - `game.created`
  - `game.ended`
  - `move.made`
  - `game.registered`
  - `achievement.acquired`

### Event Flow Patterns

#### 1. Request-Response Pattern (Session Start)
```
Platform Backend → RabbitMQ (game.session.start.requested)
  ↓
Game Service consumes → Creates session
  ↓
Game Service → RabbitMQ (game.session.started)
  ↓
Platform Backend consumes → Updates lobby
Game Logger consumes → Logs session
WebSocket Broadcaster consumes → Notifies clients
```

#### 2. Publish-Subscribe Pattern (Move Applied)
```
Game Service / Chess Backend → RabbitMQ (game.move.applied)
  ↓
Multiple consumers (all receive message):
  - Game Logger → Logs move
  - WebSocket Broadcaster → Broadcasts to clients
  - Achievement System → Evaluates achievements
```

#### 3. Fanout Pattern (Session Ended)
```
Game Service → RabbitMQ (game.session.ended)
  ↓
Topic exchange routes to fanout exchange
  ↓
Fanout exchange broadcasts to all bound queues:
  - game.session.ended.lobby → Platform Backend (lobby cleanup)
  - game.session.ended.achievements → Achievement System
  - game_logger.session_ended → Game Logger
  - game.websocket.session_ended → WebSocket Broadcaster
```

### Queue Naming Strategy

- **Service-specific queues**: Each service has unique queue names to avoid competing consumers
  - `game_logger.session_started`
  - `game.websocket.move_applied`
  - `game.session.ended.lobby`
- **Routing keys**: Shared across services for event routing
- **Fanout queues**: Separate queues for fanout exchange to ensure all consumers receive messages

---

## Event Handlers and Consumers

### Platform Backend Event Handlers

#### 1. ChessGameLobbyHandler
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/messaging/ChessGameLobbyHandler.java`
- **Events Consumed**:
  - `PlayerJoinedLobbyEvent` (Spring Event)
  - `LobbyStartedEvent` (Spring Event)
- **Responsibilities**:
  - Preregisters chess players when lobby becomes full
  - Creates chess game in external backend when lobby starts
  - Publishes `game.session.start.requested` event

#### 2. ChessGameACLAdapter
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/ChessGameACLAdapter.java`
- **Events Consumed** (from `gameExchange`):
  - `game.created`
  - `game.ended`
  - `move.made`
  - `game.registered`
  - `achievement.acquired`
- **Responsibilities**:
  - Transforms external chess events to platform format
  - Looks up player IDs by name
  - Republishes to `game_events` exchange

#### 3. GameSessionStartedConsumer
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/games/adapter/messaging/GameSessionStartedConsumer.java`
- **Events Consumed**: `game.session.started`
- **Responsibilities**:
  - Updates lobby with sessionId
  - Sets lobby status to IN_PROGRESS

#### 4. GameSessionEndedConsumer
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/games/adapter/messaging/GameSessionEndedConsumer.java`
- **Events Consumed**: `game.session.ended` (from lobby queue)
- **Responsibilities**:
  - Finds lobby by sessionId
  - Publishes `GameEndedDomainEvent` for lobby cleanup

#### 5. GameEventConsumer (Achievements)
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/achievements/adapter/messaging/GameEventConsumer.java`
- **Events Consumed**:
  - `game.session.ended` (from achievements queue)
  - `game.move.applied`
- **Responsibilities**:
  - Evaluates achievements based on game events
  - Awards achievements to players

### Game Service Event Consumers

#### 1. GameSessionEventConsumer
- **Location**: `game-service/app/modules/games/consumers/session_consumer.py`
- **Events Consumed**: `game.session.start.requested`
- **Responsibilities**:
  - Creates game sessions
  - Publishes `game.session.started` event

#### 2. GameLoggerEventConsumer
- **Location**: `game-service/app/modules/game_logger/consumers/event_consumer.py`
- **Events Consumed**:
  - `game.session.started`
  - `game.move.applied`
  - `game.session.ended`
- **Responsibilities**:
  - Logs game sessions to database
  - Logs moves with ML training data
  - Finishes sessions with final results

#### 3. GameWebSocketEventConsumer
- **Location**: `game-service/app/modules/games/consumers/websocket_event_consumer.py`
- **Events Consumed**:
  - `game.session.started`
  - `game.move.applied`
  - `game.session.ended`
- **Responsibilities**:
  - Broadcasts events to WebSocket connections
  - Real-time game state updates

---

## API Endpoints and Routing

### API Gateway Routes

#### Platform Backend Routes
- **Path**: `/api/platform/**`
- **Target**: `platform-backend:8081`
- **Features**:
  - Rate limiting: 100 req/s (burst: 200)
  - Circuit breaker
  - JWT authentication
  - CORS enabled

#### Game Service Routes
- **Path**: `/api/v1/games/**`, `/api/v1/chatbot/**`, `/api/v1/ai-player/**`
- **Target**: `game-service:8000`
- **Features**:
  - Rate limiting: 200 req/s (burst: 400)
  - Circuit breaker
  - CORS enabled

#### Chess Backend Routes (Direct)
- **Path**: `/api/games/**`, `/api/moves/**`
- **Target**: `chess-backend:8080`
- **Features**:
  - Direct routing (no ACL transformation)
  - Circuit breaker
  - CORS enabled

#### Chess Backend Routes (ACL)
- **Path**: `/api/external/chess/games/**`, `/api/external/chess/moves/**`
- **Target**: `chess-backend:8080`
- **Features**:
  - ACL transformation (format conversion)
  - Rate limiting
  - Circuit breaker

### Key API Endpoints

#### Lobby Management
- `POST /api/platform/lobbies` - Create lobby
- `POST /api/platform/lobbies/{id}/join` - Join lobby
- `POST /api/platform/lobbies/{id}/start` - Start lobby (triggers game start)
- `GET /api/platform/lobbies/{id}` - Get lobby details
- `GET /api/platform/lobbies/{id}/external-game-instance` - Get external game instance (for chess)

#### Game Session Management
- `POST /api/v1/games/sessions/{sessionId}/moves` - Apply move (Connect Four)
- `GET /api/v1/games/sessions/{sessionId}` - Get session state
- `POST /api/v1/games/sessions/{sessionId}/abandon` - Abandon session

#### Chess Game Management
- `POST /api/games/{gameId}` - Create/activate chess game
- `PUT /api/games/{gameId}` - Preregister players
- `POST /api/moves/{gameId}` - Make chess move
- `GET /api/games/{gameId}` - Get chess game state

#### Game Logger
- `POST /api/v1/game-logger/sessions` - Create session (direct API)
- `POST /api/v1/game-logger/sessions/{sessionId}/finish` - Finish session
- `GET /api/v1/game-logger/sessions/{sessionId}/moves` - Get session moves

---

## Database Interactions

### Platform Backend Database (PostgreSQL)

#### Tables
- **lobbies**: Lobby information, status, sessionId
- **players**: Player accounts
- **achievements**: Achievement definitions
- **player_achievements**: Player achievement awards
- **external_game_instances**: Mapping of lobbyId → external game instance ID

#### Key Operations
- **Lobby Creation**: Insert into `lobbies` table
- **Lobby Update**: Update `lobbies` with sessionId, status
- **Player Lookup**: Query `players` by username
- **Achievement Award**: Insert into `player_achievements`

### Game Service Database (PostgreSQL)

#### Tables
- **game_sessions**: Game session state (game-service DB)
- **session_logs**: ML-focused session logs (game-logger DB)
- **game_moves**: ML-focused move logs (game-logger DB)

#### Key Operations
- **Session Creation**: Insert into `game_sessions` and `session_logs`
- **Move Logging**: Insert into `game_moves` with ML training data
- **Session Finish**: Update `session_logs` with winner, num_moves, end_time

### Idempotency

- **Session Creation**: `GameLogRepository.create_session()` checks if session exists before inserting
- **Prevents**: `UniqueViolation` errors when multiple events trigger session creation

---

## External Service Integration (Chess Backend)

### Integration Pattern

The chess backend is an **external service** that manages its own game state. The platform integrates with it via:

1. **REST API Calls**: Direct HTTP calls to chess backend
2. **RabbitMQ Events**: Chess backend publishes events, platform consumes and transforms

### Chess Game Lifecycle

#### 1. Preregistration (When Lobby Becomes Full)
```
Platform Backend → Chess Backend
PUT /api/games/{chessGameId}
{
  whitePlayerId: <player1-id>,
  whitePlayerName: <player1-name>,
  blackPlayerId: <player2-id>,
  blackPlayerName: <player2-name>
}
```

#### 2. Game Creation (When Lobby Starts)
```
Platform Backend → Chess Backend
POST /api/games/{chessGameId}
{
  whitePlayerId: <player1-id>,
  whitePlayerName: <player1-name>,
  blackPlayerId: <player2-id>,
  blackPlayerName: <player2-name>
}
```

#### 3. Move Application
```
Chess Frontend → Chess Backend
POST /api/moves/{chessGameId}
{
  fromSquare: "e2",
  toSquare: "e4",
  player: "WHITE"
}
```

#### 4. Game End
```
Chess Backend → RabbitMQ
game.ended event (gameExchange)
```

### Event Transformation

Chess backend events are transformed by `ChessGameACLAdapter`:

- **Input**: Chess format (gameExchange)
- **Output**: Platform format (game_events exchange)
- **Transformation**: Player name → Player ID lookup, format conversion

---

## Anti-Corruption Layer (ACL)

### Purpose

The ACL isolates the platform from external service formats and ensures clean integration.

### Components

#### 1. ChessGameACLAdapter
- **Location**: `platform-backend/src/main/java/com/banditgames/platform/acl/adapter/ChessGameACLAdapter.java`
- **Responsibilities**:
  - Consumes chess events from `gameExchange`
  - Transforms to platform format
  - Looks up player IDs by name
  - Republishes to `game_events` exchange

#### 2. ACL Transform Filter (API Gateway)
- **Location**: Custom filter in API Gateway
- **Responsibilities**:
  - Transforms request/response formats for `/api/external/chess/**` routes
  - Converts between platform and chess service formats

### Transformation Examples

#### Chess Move Event Transformation
```java
// Input (Chess Format)
{
  gameId: UUID,
  fromSquare: "e2",
  toSquare: "e4",
  player: "WHITE",
  whitePlayer: "Player 1",
  blackPlayer: "Player 2"
}

// Output (Platform Format)
{
  game_id: "uuid-string",
  session_id: "uuid-string",
  game_type: "chess",
  player_id: "platform-player-id",
  move: {
    fromSquare: "e2",
    toSquare: "e4",
    sanNotation: "e4",
    player: "WHITE"
  },
  newGameState: {
    fen: "...",
    move_number: 1
  },
  type: "GAME_MOVE_APPLIED"
}
```

---

## WebSocket Real-time Updates

### Architecture

WebSocket connections are managed by `GameWebSocketManager` in the game service. Events from RabbitMQ are broadcast to connected clients.

### Connection Flow

```
1. Frontend → Game Service
   WebSocket: ws://game-service:8000/api/v1/games/ws/{sessionId}
   
2. Game Service: GameWebSocketManager
   - Accepts WebSocket connection
   - Stores connection in session map
   - Sends initial game state
   
3. RabbitMQ → GameWebSocketEventConsumer
   - Consumes game events
   - Enqueues events for async processing
   
4. Async Processor → GameWebSocketManager
   - Processes event queue
   - Broadcasts to all connections for that session
   
5. Frontend receives WebSocket message
   - Updates UI in real-time
```

### Event Broadcasting

#### Supported Events
- `game.session.started`: Notifies clients that game has started
- `game.move.applied`: Broadcasts move updates
- `game.session.ended`: Notifies clients that game has ended

#### Broadcasting Logic
- **Session-based**: Broadcasts to all connections for a specific `sessionId`
- **Thread-safe**: Uses async event loop for WebSocket operations
- **Queue-based**: Events are queued and processed asynchronously

### Components

#### GameWebSocketManager
- **Location**: `game-service/app/modules/games/websocket/connection_manager.py`
- **Responsibilities**:
  - Manages WebSocket connections
  - Broadcasts messages to sessions
  - Handles connection lifecycle

#### GameWebSocketEventConsumer
- **Location**: `game-service/app/modules/games/consumers/websocket_event_consumer.py`
- **Responsibilities**:
  - Consumes RabbitMQ events
  - Enqueues events for broadcasting
  - Processes event queue asynchronously

---

## Rate Limiting and Circuit Breaking

### Rate Limiting (Redis-based)

#### Implementation
- **Technology**: Spring Cloud Gateway + Redis
- **Algorithm**: Token bucket algorithm
- **Storage**: Redis (distributed)

#### Configuration
- **Platform Backend**: 100 req/s (burst: 200)
- **Game Service**: 200 req/s (burst: 400)
- **Chess Game Sessions**: 50 req/s (burst: 100)
- **Chess Game Moves**: 100 req/s (burst: 200)

#### Key Resolver
- **User-based**: Uses JWT user ID for rate limiting
- **Location**: `api-gateway/src/main/java/com/banditgames/gateway/config/RedisRateLimiterConfig.java`

### Circuit Breaking (Resilience4j)

#### Implementation
- **Technology**: Resilience4j
- **Pattern**: Circuit breaker pattern

#### Configuration
- **Sliding Window**: 10 requests
- **Failure Threshold**: 50%
- **Wait Duration**: 10s (open state)
- **Half-Open Permits**: 3

#### Fallback Behavior
- **Platform Backend**: Returns 503 with fallback message
- **Game Service**: Returns 503 with fallback message
- **Chess Backend**: Returns 503 with fallback message

---

## Summary

This document provides a comprehensive overview of the backend logical flows in the game platform system. Key takeaways:

1. **Event-Driven Architecture**: Most communication is asynchronous via RabbitMQ
2. **Service Separation**: Clear boundaries between platform, game service, and external chess service
3. **ACL Pattern**: External services are integrated via Anti-Corruption Layer
4. **Real-time Updates**: WebSocket broadcasting for live game state
5. **Resilience**: Rate limiting and circuit breaking for reliability
6. **ML Logging**: Comprehensive game logging for machine learning training

For specific implementation details, refer to the source code files mentioned in each section.


