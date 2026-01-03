# AI Player Module

## Overview

The AI Player module provides an MCTS-based AI opponent for Connect Four with 3 difficulty levels and dynamic difficulty adjustment.

## Features

- **Custom MCTS Algorithm**: Monte Carlo Tree Search with UCB1 (no external dependencies)
- **3 Difficulty Levels**: LOW, MEDIUM, HIGH
- **Dynamic Difficulty**: Auto-adjusts based on player win rate
- **Smart Blocking**: Detects and blocks opponent threats
- **Fast Response**: Optimized for real-time gameplay

## MCTS Algorithm

### What is MCTS?

Monte Carlo Tree Search is a decision-making algorithm that uses random simulations to find the best move. It's used in many game AIs including AlphaGo.

### How It Works (4 Steps)

```
1. SELECTION      →  Pick best child node using UCB1 formula
2. EXPANSION      →  Add new child node for unexplored move  
3. SIMULATION     →  Play random moves until game ends
4. BACKPROPAGATION →  Update win/visit counts back up the tree
```

### UCB1 Formula

```
UCB1 = win_rate + C × √(ln(parent_visits) / child_visits)

- win_rate      : How often we win from this node (exploitation)
- C             : Exploration constant (√2 ≈ 1.414)
- exploration   : Bonus for less-visited nodes
```

**Higher UCB1 = More promising or under-explored move**

### Code Structure

```python
# MCTSNode - Each node in the search tree
class MCTSNode:
    state         # Game state at this node
    parent        # Parent node
    move          # Move that led here (0-6)
    children      # Child nodes
    wins          # Number of wins
    visits        # Number of visits
    
    def ucb1()    # Calculate UCB1 value
    def win_rate  # wins / visits

# MCTSAlgorithm - Main search algorithm
class MCTSAlgorithm:
    def search(state, iterations)  # Run MCTS search
    def _select_child(node)        # Pick best child (UCB1)
    def _expand(node)              # Add new child
    def _simulate(state)           # Random playout
    def _backpropagate(node, result)  # Update tree
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/ai-player/move` | Get AI's best move |
| POST | `/api/v1/ai-player/adjust-difficulty` | Adjust difficulty |
| GET | `/api/v1/ai-player/levels` | List difficulty levels |

## Module Structure

```
ai_player/
├── __init__.py      # Module exports
├── models.py        # AIPlayerLevel enum, MoveResponse dataclass
├── dto.py           # Pydantic request/response models
├── service.py       # Main AI logic (get_move, adjust_difficulty)
├── mcts.py          # Custom MCTS algorithm with UCB1
├── api.py           # FastAPI route handlers
└── requests.http    # API test requests
```

## Difficulty Levels

| Level | Iterations | Description |
|-------|------------|-------------|
| LOW | 100 | Fast moves, good for beginners |
| MEDIUM | 500 | Balanced gameplay |
| HIGH | 1000 | Challenging AI opponent |

More iterations = Smarter AI = Slower response

## Dynamic Difficulty Adjustment

The AI automatically adjusts difficulty based on player performance:

- **Player win rate > 70%** → Increase difficulty (make it harder)
- **Player win rate < 30%** → Decrease difficulty (make it easier)
- **Win rate 30-70%** → Keep current level (balanced)

## Usage Example

```python
from app.modules.ai_player.service import AIPlayerService
from app.modules.ai_player.models import AIPlayerLevel

# Create service
service = AIPlayerService()

# Get AI move
game_state = {
    "board": [
        [0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0]
    ],
    "current_player": "ai_p2",
    "player_ids": ["human_p1", "ai_p2"],
    "status": "ongoing",
    "move_number": 1
}

result = service.get_move(game_state, AIPlayerLevel.MEDIUM)
print(f"AI plays column: {result.move_column}")
print(f"Thinking time: {result.thinking_time_ms}ms")

# Adjust difficulty based on player performance
adjustment = service.adjust_difficulty(
    player_win_rate=0.75,           # Player winning 75%
    current_level=AIPlayerLevel.LOW
)
print(f"New level: {adjustment['recommended_level']}")  # -> MEDIUM
```

## Board Format

| Value | Meaning |
|-------|---------|
| `0` | Empty cell |
| `1` | Player 1 (Human/X) |
| `2` | Player 2 (AI/Computer) |

## API Request Examples

### 1. List Levels (GET /api/v1/ai-player/levels)

Get all available difficulty levels with their settings.

**Request:**
```http
GET http://localhost:8000/api/v1/ai-player/levels
Accept: application/json
```

**Response:**
```json
{
    "levels": [
        {"level": "low", "iterations": 100},
        {"level": "medium", "iterations": 500},
        {"level": "high", "iterations": 1000}
    ]
}
```

---

### 2. Get AI Move - Compare All 3 Difficulty Levels

Test the same board position with different AI strength levels.

#### 2.1 LOW Level (100 iterations) - Fast but weak

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 1, 0, 0, 0],
            [0, 0, 1, 2, 2, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 5
    },
    "level": "low"
}
```

**Response:**
```json
{
    "move_column": 2,
    "thinking_time_ms": 45.3,
    "level_used": "low"
}
```

#### 2.2 MEDIUM Level (500 iterations) - Balanced

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 1, 0, 0, 0],
            [0, 0, 1, 2, 2, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 5
    },
    "level": "medium"
}
```

**Response:**
```json
{
    "move_column": 5,
    "thinking_time_ms": 156.7,
    "level_used": "medium"
}
```

#### 2.3 HIGH Level (1000 iterations) - Strong but slower

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 1, 0, 0, 0],
            [0, 0, 1, 2, 2, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 5
    },
    "level": "high"
}
```

**Response:**
```json
{
    "move_column": 5,
    "thinking_time_ms": 312.4,
    "level_used": "high"
}
```

---

### 3. Dynamic Difficulty Adjustment

Automatically adjust AI difficulty based on player performance.

#### 3.1 Player Winning Too Much (80%) - Increase Difficulty

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/adjust-difficulty
Content-Type: application/json

{
    "player_win_rate": 0.80,
    "current_level": "low"
}
```

**Response:**
```json
{
    "previous_level": "low",
    "recommended_level": "medium",
    "reason": "Player win rate 80.0% > 70% - increasing difficulty"
}
```

#### 3.2 Player Still Winning (75%) - Increase Again

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/adjust-difficulty
Content-Type: application/json

{
    "player_win_rate": 0.75,
    "current_level": "medium"
}
```

**Response:**
```json
{
    "previous_level": "medium",
    "recommended_level": "high",
    "reason": "Player win rate 75.0% > 70% - increasing difficulty"
}
```

#### 3.3 Balanced Game (50%) - Keep Same Level

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/adjust-difficulty
Content-Type: application/json

{
    "player_win_rate": 0.50,
    "current_level": "medium"
}
```

**Response:**
```json
{
    "previous_level": "medium",
    "recommended_level": "medium",
    "reason": "Player win rate 50.0% is balanced - no change needed"
}
```

#### 3.4 Player Losing Too Much (20%) - Decrease Difficulty

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/adjust-difficulty
Content-Type: application/json

{
    "player_win_rate": 0.20,
    "current_level": "high"
}
```

**Response:**
```json
{
    "previous_level": "high",
    "recommended_level": "medium",
    "reason": "Player win rate 20.0% < 30% - decreasing difficulty"
}
```

#### 3.5 Player Still Losing (25%) - Decrease Again

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/adjust-difficulty
Content-Type: application/json

{
    "player_win_rate": 0.25,
    "current_level": "medium"
}
```

**Response:**
```json
{
    "previous_level": "medium",
    "recommended_level": "low",
    "reason": "Player win rate 25.0% < 30% - decreasing difficulty"
}
```

---

### 4. AI Intelligence - Blocking Threats

Demonstrates the AI's ability to detect and block opponent threats.

#### 4.1 Horizontal Threat - Block 3 in a Row

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 2, 0, 0, 0],
            [0, 0, 0, 2, 0, 0, 0],
            [0, 1, 1, 1, 0, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 7
    },
    "level": "high"
}
```

**Response:**
```json
{
    "move_column": 0,
    "thinking_time_ms": 289.1,
    "level_used": "high"
}
```

**Expected:** AI should block at column 0 or 4 (Player 1 has 3 in a row at columns 1,2,3)

#### 4.2 Vertical Threat - Block 3 Stacked

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 1, 0, 0, 0],
            [2, 0, 0, 1, 0, 0, 0],
            [2, 0, 0, 1, 0, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 7
    },
    "level": "high"
}
```

**Response:**
```json
{
    "move_column": 3,
    "thinking_time_ms": 301.5,
    "level_used": "high"
}
```

**Expected:** AI should block at column 3 (Player 1 has 3 stacked vertically in column 3)

#### 4.3 Diagonal Threat - Block 3 Diagonal

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 1, 0, 2, 0, 0, 0],
            [0, 0, 1, 2, 0, 0, 0],
            [0, 0, 0, 1, 0, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 7
    },
    "level": "high"
}
```

**Response:**
```json
{
    "move_column": 0,
    "thinking_time_ms": 295.8,
    "level_used": "high"
}
```

**Expected:** AI should block the diagonal pattern (Player 1 has 3 diagonal pieces)

---

### 5. Empty Board - First Move

**Request:**
```http
POST http://localhost:8000/api/v1/ai-player/move
Content-Type: application/json

{
    "game_state": {
        "board": [
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0],
            [0, 0, 0, 0, 0, 0, 0]
        ],
        "current_player": "ai_p2",
        "player_ids": ["human_p1", "ai_p2"],
        "status": "ongoing",
        "move_number": 1
    },
    "level": "medium"
}
```

**Response:**
```json
{
    "move_column": 3,
    "thinking_time_ms": 98.2,
    "level_used": "medium"
}
```

**Expected:** AI typically plays center column (3) as optimal first move

---

### Request/Response Field Reference

**Request Fields:**
- `board`: 6x7 grid (0=empty, 1=player1, 2=player2/AI)
- `current_player`: "human_p1" or "ai_p2"
- `player_ids`: Array of player identifiers (e.g., `["human_p1", "ai_p2"]`)
- `status`: "ongoing", "win_p1", "win_p2", or "draw"
- `move_number`: Current move count
- `level`: "low", "medium", or "high"

**Response Fields:**
- `move_column`: Column index (0-6) where AI places piece
- `thinking_time_ms`: Computation time in milliseconds
- `level_used`: Difficulty level used for calculation

---

## Demo Scenarios

The AI Player module includes comprehensive demonstration scenarios in [requests.http](requests.http) that showcase:

### Part 1: Multiple Skill Levels

Compare how the same board position is evaluated at different difficulty levels:

- **LOW** (100 iterations): Fast responses, good for beginners
- **MEDIUM** (500 iterations): Balanced gameplay
- **HIGH** (1000 iterations): Strong tactical play, blocks threats effectively

Example board state used for comparison:
```
Row 5: [0, 0, 1, 2, 2, 0, 0]
Row 4: [0, 0, 0, 1, 0, 0, 0]
```

Each level will analyze this position differently based on simulation depth.

### Part 2: Dynamic Difficulty Examples

Real-world scenarios showing automatic difficulty adjustment:

| Scenario | Win Rate | Current Level | Recommended | Reason |
|----------|----------|---------------|-------------|---------|
| Player dominating | 80% | LOW | MEDIUM | Win rate > 70% |
| Still winning | 75% | MEDIUM | HIGH | Win rate > 70% |
| Balanced match | 50% | MEDIUM | MEDIUM | Win rate 30-70% |
| Player struggling | 20% | HIGH | MEDIUM | Win rate < 30% |
| Still losing | 25% | MEDIUM | LOW | Win rate < 30% |

### Part 3: AI Intelligence - Threat Detection

Demonstrates the AI's ability to detect and block opponent threats:

**3.1 Horizontal Threat**
```
Row 5: [0, 1, 1, 1, 0, 0, 0]  ← Player 1 has 3 in a row!
Row 4: [0, 0, 0, 2, 0, 0, 0]
Row 3: [0, 0, 0, 2, 0, 0, 0]
```
Expected AI move: Column 0 or 4 (blocks the threat)

**3.2 Vertical Threat**
```
Row 5: [2, 0, 0, 1, 0, 0, 0]
Row 4: [2, 0, 0, 1, 0, 0, 0]
Row 3: [0, 0, 0, 1, 0, 0, 0]  ← Player 1 has 3 stacked!
```
Expected AI move: Column 3 (blocks vertical threat)

**3.3 Diagonal Threat**
```
Row 5: [0, 0, 0, 1, 0, 0, 0]
Row 4: [0, 0, 1, 2, 0, 0, 0]
Row 3: [0, 1, 0, 2, 0, 0, 0]  ← Player 1 has 3 diagonal!
```
Expected AI move: Blocks the diagonal pattern

### Part 4: Empty Board (First Move)

On an empty board, the AI typically plays the center column (3) as it provides the most strategic options.

---

## Testing with cURL

```bash
# Get AI move
curl -X POST http://localhost:8000/api/v1/ai-player/move \
  -H "Content-Type: application/json" \
  -d '{"game_state":{"board":[[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0]],"current_player":"ai_p2","player_ids":["human_p1","ai_p2"],"status":"ongoing","move_number":1},"level":"medium"}'

# Adjust difficulty
curl -X POST http://localhost:8000/api/v1/ai-player/adjust-difficulty \
  -H "Content-Type: application/json" \
  -d '{"player_win_rate":0.75,"current_level":"low"}'

# List levels
curl http://localhost:8000/api/v1/ai-player/levels
```

## Dependencies

- `fastapi`: Web framework
- `pydantic`: Data validation

**No external AI libraries required!** MCTS is implemented from scratch.

---

## Summary

The AI Player module provides a complete Connect Four AI solution:

- 3 Skill Levels: LOW (100), MEDIUM (500), HIGH (1000) iterations
- Dynamic Difficulty: Auto-adjusts based on win rate (>70% harder, <30% easier)
- Smart AI: Blocks horizontal, vertical, and diagonal threats
- Board Format: 0 = empty, 1 = player 1, 2 = player 2 (AI)
- Fast Response: Optimized MCTS algorithm with no external dependencies
- Real-time Gameplay: Suitable for interactive web applications

For complete API testing examples, see [requests.http](requests.http).

