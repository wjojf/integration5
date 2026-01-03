# Game Logger Module

## Overview

The Game Logger module handles logging of gameplay data for analytics and ML training.

## Features

- Log all game moves and states
- Store data in PostgreSQL
- Track game sessions
- Support data export for ML training

## API Endpoints

- `POST /api/v1/game-logger/log-move` - Log a move
- `POST /api/v1/game-logger/create-session` - Create game session
- `POST /api/v1/game-logger/finish-session` - Finish game session
- `GET /api/v1/game-logger/game/{game_id}/logs` - Get game logs
- `GET /api/v1/game-logger/player/{player_id}/logs` - Get player logs

## Module Structure

```
game_logger/
├── __init__.py      # Module setup
├── models.py        # Database models (GameLog, GameSession)
├── dto.py           # API request/response models
├── service.py       # Logging service
└── api.py           # FastAPI route handlers
```

## Database Schema

### game_logs
Stores individual move logs with:
- Game state before/after move
- Player information
- MCTS metadata (if applicable)
- Move quality scores

### game_sessions
Stores game session metadata:
- Player information
- Final result
- Duration and statistics

## Configuration

```env
MODULE_GAME_LOGGER_ENABLED=true
LOGGER_ENABLED=true
LOGGER_BATCH_SIZE=100
LOGGER_FLUSH_INTERVAL_SECONDS=5

# Database connection
DB_HOST=localhost
DB_PORT=5432
DB_NAME=game_service
DB_USER=postgres
DB_PASSWORD=postgres
```

## Usage Example

```python
from app.modules.game_logger.service import GameLoggerService

service = GameLoggerService()
service.initialize()  # Create tables

log = service.log_move(
    game_id="game-1",
    move_index=1,
    player_id="player-1",
    agent_type="human",
    state_before=state_before_dict,
    move_column=3,
    state_after=state_after_dict,
)
```


