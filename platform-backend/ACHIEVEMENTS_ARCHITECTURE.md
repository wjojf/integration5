# Achievements System Architecture

## Overview

The achievements system is implemented following Clean Architecture and Event-Driven Architecture (EDA) principles. It automatically evaluates and unlocks achievements when game events occur, supporting both chess and connect four games, with an expandable design for future games.

## Architecture Layers

### 1. Domain Layer (`domain/`)
- **Achievement**: Core domain model for achievements
- **UserAchievement**: Domain model for player achievements
- **PlayerStatistics**: Domain model for tracking player performance metrics
- **AchievementEvaluator**: Strategy interface for evaluating achievements
- **AchievementCriteria**: Enum for achievement criteria types (COUNTER_REACHES_THRESHOLD, STREAK, ONE_TIME_EVENT, TIME_REACHED)
- **TriggeringEventType**: Enum for event types that trigger evaluation (GAME_WON, GAMES_LOST, TIME_PASSED)

### 2. Use Case Layer (`usecase/`)
- **EvaluateAchievementsService**: Main service that evaluates achievements based on game events
- **SavePlayerAcquiredNewAchievementService**: Service for saving unlocked achievements

### 3. Port Layer (`port/`)
- **In Ports (Use Cases)**:
  - `EvaluateAchievementsUseCase`: Interface for achievement evaluation
  - `SavePlayerAcquiredNewAchievementUseCase`: Interface for saving achievements
- **Out Ports (Adapters)**:
  - `LoadAchievementsPort`: Loads achievements from persistence
  - `LoadPlayerStatisticsPort`: Loads/updates player statistics
  - `SaveUserAchievementPort`: Saves user achievements

### 4. Adapter Layer (`adapter/`)
- **Messaging Adapters**:
  - `GameEventConsumer`: Consumes game events from RabbitMQ (`game.session.ended`, `game.move.applied`)
- **Persistence Adapters**:
  - `AchievementPersistenceAdapter`: Loads achievements from database
  - `PlayerStatisticsPersistenceAdapter`: Manages player statistics
  - `UserAchievementPersistenceAdapter`: Saves user achievements

## Event Flow

```
Game Service / External Game
    ↓ (publishes event)
RabbitMQ (game_events exchange)
    ↓ (routing key: game.session.ended or game.move.applied)
GameEventConsumer
    ↓ (delegates to)
EvaluateAchievementsService
    ↓ (loads achievements & statistics)
Achievement Evaluators (Strategy Pattern)
    ↓ (evaluates criteria)
SavePlayerAcquiredNewAchievementService
    ↓ (saves to database)
UserAchievementEntity
```

## Achievement Evaluators

The system uses a Strategy pattern with multiple evaluators:

1. **CounterAchievementEvaluator**: Evaluates counter-based achievements (e.g., "Win 10 games")
2. **StreakAchievementEvaluator**: Evaluates streak achievements (e.g., "Win 10 games in a row")
3. **OneTimeEventAchievementEvaluator**: Evaluates one-time events (e.g., "First Victory")
4. **TimeBasedAchievementEvaluator**: Evaluates time-based achievements (e.g., "Win under 2 minutes", "Play for 1 hour")
5. **SocialAchievementEvaluator**: Evaluates social achievements (e.g., "Play against 20 unique players")

## Player Statistics

The system tracks the following statistics per player per game:
- Win/Loss/Draw counts
- Current and longest win streaks
- Total play time
- Fastest win time
- Unique opponents played against
- Total moves and games played

## Database Schema

### `achievements` table
- Stores achievement definitions
- Includes `criteria` and `triggering_event_type` columns for evaluation

### `player_statistics` table
- Stores player performance metrics per game
- Used for achievement evaluation

### `user_achievements` table
- Stores unlocked achievements for players
- Prevents duplicate unlocks

## Adding Achievements for a New Game

### Step 1: Create Achievement Records

Add achievement records to the database with appropriate criteria:

```sql
INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES (
    gen_random_uuid(),
    'your-game-id',
    'Achievement Name',
    'Achievement description that matches evaluator patterns',
    'PROGRESSION',  -- or TIME, DIFFICULTY, SOCIAL
    'COMMON',       -- or UNCOMMON, RARE, EPIC, LEGENDARY
    'COUNTER_REACHES_THRESHOLD',  -- or STREAK, ONE_TIME_EVENT, TIME_REACHED
    'GAME_WON',     -- or GAMES_LOST, TIME_PASSED
    'Win 5 games.', -- Description that evaluators can parse
    false
);
```

### Step 2: Ensure Game Events Are Published

Make sure your game publishes events to RabbitMQ:
- `game.session.ended` - Published when a game ends
- `game.move.applied` - Published when a move is made (optional, for move-based achievements)

Event format should include:
- `game_id`: UUID of the game
- `game_type`: String identifier (e.g., "chess", "connect_four")
- `player_ids`: List of player UUIDs
- `winner_id`: UUID of winner (null for draws)
- `session_id`: Session identifier

### Step 3: (Optional) Create Game-Specific Evaluator

If you need game-specific achievement logic, create a new evaluator:

```java
@Component
public class MyGameAchievementEvaluator implements AchievementEvaluator {
    
    @Override
    public boolean evaluate(
            Achievement achievement,
            UUID playerId,
            PlayerStatistics statistics,
            EvaluationContext context
    ) {
        // Custom evaluation logic
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        // Return true if this evaluator handles this achievement
        return achievement.getGameId().equals(MY_GAME_ID);
    }
}
```

The evaluator will be automatically discovered and used by the evaluation service.

## Configuration

### Application Properties

```properties
# Game Events Configuration
game.events.exchange.name=game_events
game.events.queues.session-ended=game.session.ended
game.events.queues.move-applied=game.move.applied
```

### RabbitMQ Setup

The system expects:
- Exchange: `game_events` (topic exchange)
- Queues bound to routing keys:
  - `game.session.ended` → `game.session.ended` queue
  - `game.move.applied` → `game.move.applied` queue

## Testing Achievements

1. **Start a game** and complete it
2. **Check player statistics**:
   ```sql
   SELECT * FROM player_statistics WHERE player_id = 'your-player-id' AND game_id = 'your-game-id';
   ```
3. **Check unlocked achievements**:
   ```sql
   SELECT * FROM user_achievements WHERE user_id = 'your-player-id';
   ```

## Best Practices

1. **Achievement Descriptions**: Use clear, parseable descriptions that match evaluator patterns:
   - "Win 10 games" (counter)
   - "Win 10 games in a row" (streak)
   - "Win a game under 2 minutes" (time-based)
   - "Play against 20 unique players" (social)

2. **Criteria Selection**:
   - Use `COUNTER_REACHES_THRESHOLD` for cumulative achievements
   - Use `STREAK` for consecutive achievements
   - Use `ONE_TIME_EVENT` for first-time achievements
   - Use `TIME_REACHED` for time-based achievements

3. **Event Publishing**: Ensure game events include all necessary information:
   - Player IDs
   - Winner ID (if applicable)
   - Game duration (for time-based achievements)
   - Opponent IDs (for social achievements)

## Extensibility

The system is designed to be easily extensible:

1. **New Evaluators**: Simply implement `AchievementEvaluator` and annotate with `@Component`
2. **New Criteria Types**: Add to `AchievementCriteria` enum and create corresponding evaluator
3. **New Event Types**: Add to `TriggeringEventType` enum and handle in `EvaluateAchievementsService`
4. **Game-Specific Logic**: Create game-specific evaluators that check `gameId` or `gameType`

## Clean Architecture Compliance

- ✅ **Dependency Rule**: Dependencies point inward (adapters depend on domain, not vice versa)
- ✅ **Interface Segregation**: Ports are focused and specific
- ✅ **Dependency Inversion**: Use cases depend on port interfaces, not implementations
- ✅ **Separation of Concerns**: Domain logic separated from infrastructure

## EDA Compliance

- ✅ **Event-Driven**: Achievements are evaluated asynchronously based on events
- ✅ **Loose Coupling**: Services communicate via events, not direct calls
- ✅ **Scalability**: Event consumers can be scaled independently
- ✅ **Resilience**: Event processing failures don't break game flow

