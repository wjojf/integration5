# ML Models Module

This module provides two machine learning models for Connect Four:

1. **Policy Model** - Predicts the best move to play (MLP Classifier)
2. **Win Probability Model** - Predicts chance of winning (MLP Regressor)

## Architecture

```
ml_models/
├── __init__.py
├── api.py              # FastAPI endpoints
├── service.py          # Training & inference service
├── policy_model.py     # Policy imitation model (MLP Classifier)
├── winprob_model.py    # Win probability model (MLP Regressor)
├── data_utils.py       # Data loading and preprocessing
└── requests.http       # API test file
```

## Quick Start

### 1. Generate Training Data

```bash
# Run self-play to generate games
docker exec game-service python -m scripts.self_play --games 100

# Export to parquet
curl -X POST "http://localhost:8000/api/v1/game-logger/export?version=v1"
```

### 2. Train Models via API

```bash
# Train policy model
curl -X POST "http://localhost:8000/api/v1/ml-models/train/policy" \
  -H "Content-Type: application/json" \
  -d '{"dataset_path": "/app/data/datasets/v1/game_logs_v1_latest.parquet"}'

# Train win probability model
curl -X POST "http://localhost:8000/api/v1/ml-models/train/winprob" \
  -H "Content-Type: application/json" \
  -d '{"dataset_path": "/app/data/datasets/v1/game_logs_v1_latest.parquet"}'

# Train both models
curl -X POST "http://localhost:8000/api/v1/ml-models/train/all" \
  -H "Content-Type: application/json" \
  -d '{"dataset_path": "/app/data/datasets/v1/game_logs_v1_latest.parquet"}'
```

### 3. Use Prediction Endpoints

```bash
# Check model status
curl http://localhost:8000/api/v1/ml-models/status

# Predict best move
curl -X POST "http://localhost:8000/api/v1/ml-models/predict/move" \
  -H "Content-Type: application/json" \
  -d '{"board_state": "[[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[1,0,0,2,0,0,0]]", "player": 1}'

# Predict win probability
curl -X POST "http://localhost:8000/api/v1/ml-models/predict/winprob" \
  -H "Content-Type: application/json" \
  -d '{"board_state": "[[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[0,0,0,0,0,0,0],[1,0,0,2,0,0,0]]", "player": 1}'
```

## API Endpoints

Base URL: `http://localhost:8000/api/v1/ml-models`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `GET` | `/status` | Model status and metrics |
| `POST` | `/train/policy` | Train policy model |
| `POST` | `/train/winprob` | Train win probability model |
| `POST` | `/train/all` | Train both models |
| `POST` | `/predict/move` | Predict best move |
| `POST` | `/predict/winprob` | Predict win probability |

### Training Request Body

```json
{
  "dataset_path": "/app/data/datasets/v1/game_logs_v1_latest.parquet",
  "hidden_layers": [128, 64, 32],  // Optional, default: [128, 64, 32]
  "max_iter": 500                   // Optional, default: 500
}
```

### Prediction Request Body

```json
{
  "board_state": "[[0,0,0,0,0,0,0],...]",  // 6x7 board as JSON string
  "player": 1                               // Current player (1 or 2)
}
```

## Model Details

### Policy Model (MLPClassifier)

- **Input**: 126 features (3 channels × 6 rows × 7 cols)
  - Channel 0: Current player's pieces
  - Channel 1: Opponent's pieces
  - Channel 2: Empty cells
- **Output**: Move probabilities for columns 0-6
- **Architecture**: MLP with configurable hidden layers (default: 128→64→32)
- **Training**: Adam optimizer with early stopping

### Win Probability Model (MLPRegressor)

- **Input**: 133 features (126 board features + 7 move one-hot)
- **Output**: Win probability (0.0 - 1.0)
- **Target Labels**: Blended from game outcome and MCTS heuristic value
  - Game outcome: 1.0 (win), 0.5 (draw), 0.0 (loss)
  - Weighted: 60% outcome + 40% heuristic

## MLFlow Integration

Models are logged to MLFlow for experiment tracking:

- **UI**: http://localhost:5000
- **Experiments**: `connect4_policy`, `connect4_winprob`
- **Logged**: Parameters, metrics, and model artifacts

## File Storage

```
data/
└── models/
    ├── policy_model.pkl      # Trained policy model
    └── winprob_model.pkl     # Trained winprob model
```
