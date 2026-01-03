# Chess Game Lobby Integration - How Two Players Play Together

This document explains how two players from the same lobby (on different clients) coordinate to play the same chess game instance.

## Current State

### ❌ Current Chess Flow (No Lobby Integration)
```
Player 1: Clicks "Play Now" → Registers game → Redirects to chess frontend
Player 2: Clicks "Play Now" → Registers game → Redirects to chess frontend
Result: Two separate games, no coordination
```

### ✅ How It Should Work (With Lobby Integration)
```
Player 1: Creates lobby → Waits for Player 2
Player 2: Joins lobby → Both players ready
Host: Starts lobby → Chess game created with both players → Both redirected to same game
Result: Both players play the same chess game instance
```

## Required Flow for Chess Lobby Integration

### Phase 1: Lobby Creation and Joining

```
┌─────────────┐                    ┌─────────────┐
│  Player 1   │                    │  Player 2   │
│ (Client 1)  │                    │ (Client 2)  │
└──────┬──────┘                    └──────┬──────┘
       │                                   │
       │ 1. Create Lobby (Chess, max 2)   │
       │ POST /api/lobbies                 │
       │ { maxPlayers: 2, private: false }│
       │                                   │
       ▼                                   │
┌─────────────────────────────────────────┐
│      Platform Backend                    │
│  - Creates lobby with status WAITING     │
│  - Player 1 is host                      │
│  - Returns lobbyId                       │
└──────┬───────────────────────────────────┘
       │
       │ 2. Lobby Created (lobbyId)
       │
       ▼
┌─────────────────────────────────────────┐
│      Player 1 Frontend                  │
│  - Displays lobby waiting screen        │
│  - Shows lobbyId for sharing             │
│  - Polls/WebSocket for player joins     │
└─────────────────────────────────────────┘

       │                                   │
       │ 3. Player 2 Joins Lobby           │
       │ POST /api/lobbies/{lobbyId}/join │
       │                                   │
       ▼                                   ▼
┌─────────────────────────────────────────┐
│      Platform Backend                    │
│  - Adds Player 2 to lobby                │
│  - Lobby now has 2 players               │
│  - Publishes PlayerJoinedLobbyEvent      │
└──────┬───────────────────────────────────┘
       │
       │ 4. Both Clients Notified
       │ (via WebSocket or polling)
       │
       ▼
┌─────────────────────────────────────────┐
│  Both Frontends                         │
│  - Show "Ready to Start"                │
│  - Host sees "Start Game" button        │
└─────────────────────────────────────────┘
```

### Phase 2: Starting the Lobby (Chess Game Creation)

```
┌─────────────┐
│  Player 1   │
│   (Host)    │
└──────┬──────┘
       │
       │ 5. Start Lobby with Chess GameId
       │ POST /api/lobbies/{lobbyId}/start
       │ { gameId: <chess-game-uuid> }
       │
       ▼
┌─────────────────────────────────────────┐
│      Platform Backend                   │
│  StartLobbyService.startLobby()         │
│  - Validates host                       │
│  - Sets lobby.gameId = chess gameId     │
│  - Sets lobby.status = STARTED          │
│  - Publishes LobbyStartedEvent          │
│    { lobbyId, gameId, playerIds: [p1, p2] }│
└──────┬───────────────────────────────────┘
       │
       │ 6. LobbyStartedEvent Published
       │
       ▼
┌─────────────────────────────────────────┐
│  GameSessionRequestPublisher             │
│  (EventListener)                         │
│                                          │
│  Checks: Is this a chess game?          │
│  - Query game registry by gameId        │
│  - If gameType == "chess":               │
│    → Handle chess game creation         │
└──────┬───────────────────────────────────┘
       │
       │ 7a. For Chess: Create Chess Game
       │
       ▼
┌─────────────────────────────────────────┐
│  ChessGameLobbyHandler (NEW)           │
│  - Registers chess game with platform   │
│  - Creates chess game in chess backend  │
│    with both player IDs                 │
│  - Stores gameId → chessGameId mapping │
│  - Publishes ChessLobbyStartedEvent     │
└──────┬───────────────────────────────────┘
       │
       │ 7b. For Native Games (Connect Four)
       │
       ▼
┌─────────────────────────────────────────┐
│  GameSessionRequestPublisher             │
│  - Publishes game.session.start.requested│
│  - Game Service creates session          │
└─────────────────────────────────────────┘
```

### Phase 3: Chess Game Creation and Player Notification

```
┌─────────────────────────────────────────┐
│  ChessGameLobbyHandler                  │
│                                          │
│  1. Register Chess Game                 │
│     POST /api/external/chess/register/{gameId}│
│     → Gets chessGameId (UUID)           │
│                                          │
│  2. Create Chess Game with Players      │
│     POST /api/games/{chessGameId}      │
│     {                                   │
│       whitePlayerId: player1Id,         │
│       whitePlayerName: "Player 1",      │
│       blackPlayerId: player2Id,         │
│       blackPlayerName: "Player 2"      │
│     }                                   │
│     → Chess backend creates game        │
│     → Publishes GameCreatedMessage      │
│                                          │
│  3. Store Mapping                       │
│     lobbyId → chessGameId               │
│     (for later reference)               │
│                                          │
│  4. Publish ChessLobbyStartedEvent      │
│     {                                   │
│       lobbyId,                          │
│       chessGameId,                      │
│       playerIds: [p1, p2],             │
│       chessFrontendUrl                  │
│     }                                   │
└──────┬───────────────────────────────────┘
       │
       │ 8. Event Published to WebSocket/Polling
       │
       ▼
┌─────────────────────────────────────────┐
│  Both Player Frontends                  │
│  (via WebSocket or polling)             │
│                                          │
│  Receive: ChessLobbyStartedEvent         │
│  {                                       │
│    chessGameId: "uuid",                 │
│    chessFrontendUrl: "http://localhost:3333/game/{chessGameId}"│
│  }                                       │
│                                          │
│  Both redirect to:                      │
│  window.location.href = chessFrontendUrl│
└─────────────────────────────────────────┘
```

### Phase 4: Both Players Connect to Same Game

```
┌─────────────┐                    ┌─────────────┐
│  Player 1   │                    │  Player 2   │
│ (Client 1)  │                    │ (Client 2)  │
└──────┬──────┘                    └──────┬──────┘
       │                                   │
       │ 9. Both navigate to                │
       │ http://localhost:3333/game/{chessGameId}│
       │                                   │
       ▼                                   ▼
┌─────────────────────────────────────────┐
│      Chess Frontend (External)          │
│                                          │
│  - Both clients load same gameId        │
│  - Chess frontend calls:                │
│    GET /api/games/{chessGameId}         │
│  - Gets same game state                 │
│  - Both see same board                  │
│  - Player 1 is WHITE, Player 2 is BLACK│
│  - Real-time updates via WebSocket      │
└─────────────────────────────────────────┘
```

## Implementation Requirements

### 1. Backend: Chess Lobby Handler

Create a new component to handle chess lobby starts:

```java
@Component
@RequiredArgsConstructor
public class ChessGameLobbyHandler {
    
    private final ChessGameController chessGameController;
    private final RabbitTemplate rabbitTemplate;
    private final LoadPlayerPort loadPlayerPort;
    
    @EventListener
    public void onLobbyStarted(LobbyStartedEvent event) {
        // Check if this is a chess game
        if (isChessGame(event.gameId())) {
            handleChessLobbyStart(event);
        }
    }
    
    private void handleChessLobbyStart(LobbyStartedEvent event) {
        // 1. Register chess game
        UUID chessGameId = registerChessGame(event);
        
        // 2. Get player names from player IDs
        Player player1 = loadPlayerPort.findById(event.playerIds().get(0));
        Player player2 = loadPlayerPort.findById(event.playerIds().get(1));
        
        // 3. Create chess game with both players
        createChessGame(chessGameId, player1, player2);
        
        // 4. Publish event for frontend notification
        publishChessLobbyStarted(event.lobbyId(), chessGameId, event.playerIds());
    }
}
```

### 2. Backend: Detect Chess Game Type

Update `GameSessionRequestPublisher.determineGameType()`:

```java
private String determineGameType(UUID gameId) {
    // Query game registry to get game type
    // For chess, game title would be "Chess" or gameType = "chess"
    Game game = gameRegistryPort.findById(gameId);
    if (game != null && "chess".equalsIgnoreCase(game.getType())) {
        return "chess";
    }
    return "connect_four"; // default
}
```

### 3. Backend: WebSocket/Polling for Lobby Events

Add WebSocket support or polling endpoint to notify players:

```java
@RestController
@RequestMapping("/api/lobbies/{lobbyId}/events")
public class LobbyEventsController {
    
    @GetMapping("/status")
    public ResponseEntity<LobbyStatusResponse> getLobbyStatus(@PathVariable UUID lobbyId) {
        // Return current lobby status
        // Frontend polls this endpoint
    }
}
```

### 4. Frontend: Lobby Flow for Chess

Modify `GameLibrary.tsx` to create lobby instead of direct redirect:

```typescript
const handlePlayGame = async (game: Game) => {
  if (isChessGame(game)) {
    // Create lobby for chess
    const lobby = await lobbyService.create({
      maxPlayers: 2,
      private: false
    });
    
    // Navigate to lobby page
    navigate(`/app/lobbies/${lobby.id}`);
  }
}
```

### 5. Frontend: Lobby Page with Start Game

Update `Lobbies.tsx` to handle starting chess games:

```typescript
const handleStartLobby = async (lobby: Lobby, gameId: string) => {
  await startLobby(lobby.id, gameId);
  
  // Poll for lobby status or use WebSocket
  // When lobby.status === "STARTED" and chessGameId exists:
  const chessGameId = lobby.chessGameId; // from response
  window.location.href = `http://localhost:3333/game/${chessGameId}`;
}
```

### 6. Chess Backend: Accept Player IDs

Modify chess backend to accept player IDs in game creation:

```java
@PostMapping("/api/games/{gameId}")
public ResponseEntity<GameResponse> createGame(
    @PathVariable UUID gameId,
    @RequestBody CreateGameRequest request) {
    
    // Accept both player IDs and names
    // request.whitePlayerId (UUID)
    // request.blackPlayerId (UUID)
    // request.whitePlayerName (String)
    // request.blackPlayerName (String)
    
    // Store player IDs for later use in events
}
```

## Key Coordination Points

### 1. **Shared Game ID**
- When lobby starts, a single `chessGameId` is generated
- Both players receive the same `chessGameId`
- Both redirect to the same chess frontend URL

### 2. **Player Identification**
- Player IDs are passed from platform to chess backend
- Chess backend stores player IDs
- Chess events include player IDs (already implemented)

### 3. **Real-time Synchronization**
- Chess frontend uses WebSocket for real-time updates
- Both clients receive move updates simultaneously
- Game state is synchronized via chess backend

### 4. **Event Flow**
```
Lobby Started → Chess Game Created → Both Players Notified → Both Redirect → Same Game
```

## Current Gaps

1. ❌ **No Chess Lobby Handler**: Need component to handle chess lobby starts
2. ❌ **No Game Type Detection**: `GameSessionRequestPublisher` doesn't detect chess games
3. ❌ **No Frontend Lobby Integration**: Chess bypasses lobby system
4. ❌ **No Player Notification**: No way to notify players when lobby starts
5. ❌ **Chess Backend**: May need to accept player IDs in game creation

## Next Steps

1. Implement `ChessGameLobbyHandler` to intercept lobby starts for chess
2. Update `GameSessionRequestPublisher` to detect chess games
3. Add WebSocket or polling for lobby status updates
4. Modify frontend to use lobby flow for chess
5. Update chess backend to accept player IDs (if needed)

