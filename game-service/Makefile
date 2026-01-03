.PHONY: up down restart build logs clean db-reset db-init test install dev help export-data export-latest train-policy train-winprob train-all

# Default target
help:
	@echo "Available targets:"
	@echo "  make up          - Start all services in detached mode"
	@echo "  make build       - Build and start all services"
	@echo "  make down        - Stop all services"
	@echo "  make restart     - Restart all services with rebuild"
	@echo "  make logs        - Follow logs from all services"
	@echo "  make clean       - Stop services and remove volumes/orphans"
	@echo "  make db-reset    - Reset database (down -v + up)"
	@echo "  make db-init     - Initialize database (create banditgames DB)"
	@echo "  make test        - Run tests"
	@echo "  make install     - Install dependencies"
	@echo "  make dev         - Start services and show logs"
	@echo "  make self-play   - Run self-play script"
	@echo ""
	@echo "Data Export & ML Training:"
	@echo "  make export-data    - Export game logs to parquet (with filters)"
	@echo "  make export-latest  - Export latest game logs"
	@echo "  make train-policy   - Train policy model from exported data"
	@echo "  make train-winprob  - Train win probability model"
	@echo "  make train-all      - Train all ML models"

# Docker compose commands
up:
	docker compose up -d

build:
	docker compose up --build -d

down:
	docker compose down

restart:
	docker compose down && docker compose up --build -d

logs:
	docker compose logs -f

clean:
	docker compose down -v --remove-orphans
	docker system prune -f

# Database management
db-reset:
	@echo "Resetting database..."
	docker compose down -v
	docker compose up -d postgres
	@echo "Waiting for database to be ready..."
	@sleep 5
	@echo "Database reset complete!"

db-init:
	@echo "Initializing database..."
	@docker exec game-service-postgres psql -U postgres -c "CREATE DATABASE banditgames;" 2>nul && echo "Database 'banditgames' created successfully!" || echo "Database 'banditgames' already exists or container not running"

# Development commands
dev:
	docker compose up

install:
	pip install -r requirements.txt

test:
	pytest tests/ -v

# ML training scripts
self-play:
	python scripts/self_play.py --games 10 --p1-iterations 500 --p2-iterations 500

# Service-specific commands
api-logs:
	docker compose logs -f api

db-logs:
	docker compose logs -f postgres

# Health checks
status:
	docker compose ps
	@echo "\nAPI Health:"
	@curl -s http://localhost:8000/health || echo "API not responding"

# Data Export & ML Training
export-data:
	@echo "Exporting game logs to parquet..."
	@curl -X POST "http://localhost:8000/api/game-logger/export?limit=1000&min_games=5" \
		-H "Content-Type: application/json" || echo "Failed to export. Is API running?"

export-latest:
	@echo "Exporting latest game logs..."
	@curl -X POST "http://localhost:8000/api/game-logger/export?limit=100" \
		-H "Content-Type: application/json" || echo "Failed to export. Is API running?"

train-policy:
	@echo "Training policy model..."
	python -c "from app.modules.ml_models.service import MLModelService; \
		service = MLModelService(); \
		service.train_policy_model(experiment_name='policy_training')"

train-winprob:
	@echo "Training win probability model..."
	python -c "from app.modules.ml_models.service import MLModelService; \
		service = MLModelService(); \
		service.train_winprob_model(experiment_name='winprob_training')"

train-all: train-policy train-winprob
	@echo "All models trained successfully!"

# Complete ML workflow
ml-workflow: self-play export-latest train-all
	@echo "Complete ML workflow finished!"
	@echo "1. Generated training data with self-play"
	@echo "2. Exported data to parquet"
	@echo "3. Trained all ML models"
