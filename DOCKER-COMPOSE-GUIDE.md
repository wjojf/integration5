# Docker Compose - Quick Reference Guide

## Quick Start

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Start all core services
docker-compose up -d

# 3. Check status
docker-compose ps

# 4. View logs
docker-compose logs -f
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8080 | http://localhost:8080 |
| Platform Backend | 8081 | http://localhost:8081 |
| Game Service | 8000 | http://localhost:8000 |
| Platform Frontend | 5173 | http://localhost:5173 |
| Keycloak | 8180 | http://localhost:8180 |
| RabbitMQ Management | 15672 | http://localhost:15672 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |
| Chess Backend | 8090 | http://localhost:8090 (--profile chess) |
| Chess Frontend | 3333 | http://localhost:3333 (--profile chess) |
| MLFlow | 5000 | http://localhost:5000 (--profile ml) |
| MinIO Console | 9001 | http://localhost:9001 (--profile ml) |

## Shared Infrastructure

All services share:
- **PostgreSQL** (separate databases: `banditgames`, `game_service`, `keycloak`; chess game uses `postgres` database)
- **RabbitMQ** (shared exchange: `game_events`)
- **Redis** (for API Gateway rate limiting)
- **Keycloak** (SSO authentication)

## Profiles

### Core Services (Default)
```bash
docker-compose up -d
```
Starts: API Gateway, Platform Backend, Game Service, Platform Frontend, Infrastructure

### With External Chess Game
```bash
docker-compose --profile chess up -d
```
Also starts: Chess Game Backend, Chess Game Frontend

### With ML/AI Services
```bash
docker-compose --profile ml up -d
```
Also starts: MLFlow, MinIO

### Everything
```bash
docker-compose --profile chess --profile ml up -d
```

## Common Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Stop and remove volumes (⚠️ deletes data)
docker-compose down -v

# Rebuild specific service
docker-compose build <service-name>
docker-compose up -d --build <service-name>

# View logs
docker-compose logs -f <service-name>

# Execute command in service
docker-compose exec <service-name> <command>

# Check health
docker-compose ps
```

## Database Access

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d banditgames

# List all databases
docker-compose exec postgres psql -U postgres -l
```

## RabbitMQ Management

Access at http://localhost:15672
- Username: `admin` (default)
- Password: `admin` (default)

## Keycloak Admin

Access at http://localhost:8180
- Username: `admin` (default)
- Password: `admin` (default)

## Troubleshooting

### Port Already in Use
Update port in `.env`:
```bash
API_GATEWAY_PORT=8081  # Change from 8080
```

### Service Won't Start
1. Check logs: `docker-compose logs <service-name>`
2. Verify dependencies: `docker-compose ps`
3. Check health: `docker-compose ps` (look for "healthy" status)

### Database Connection Issues
1. Ensure PostgreSQL is healthy: `docker-compose ps postgres`
2. Check database exists: `docker-compose exec postgres psql -U postgres -l`
3. Verify connection string in service environment variables

### External Chess Game Configuration
The chess game uses pre-built images from GitLab registry. To use different images, update `.env`:
```bash
CHESS_GAME_BACKEND_IMAGE=registry.gitlab.com/kdg-ti/integration-5/chessgame/back:latest
CHESS_GAME_FRONTEND_IMAGE=registry.gitlab.com/kdg-ti/integration-5/chessgame/front:latest
CHESS_GAME_REGISTRATION_ID=8496c496-a884-48ed-9bb3-7c3aa50fb8ca
CHESS_DB_NAME=postgres  # Chess game uses 'postgres' database, not 'chess_game'
```

## Environment Variables

Key variables (see `.env.example` for full list):
- `POSTGRES_USER` / `POSTGRES_PASSWORD`: Database credentials
- `RABBITMQ_USER` / `RABBITMQ_PASSWORD`: RabbitMQ credentials  
- `SERVICE_API_KEY`: Service-to-service auth key
- `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`: Keycloak admin
- `CHESS_GAME_BACKEND_PATH`: Path to chess backend (if external)

## Network

All services communicate via `banditgames-network` using service names:
- `http://platform-backend:8081`
- `http://game-service:8000`
- `http://chess-game-backend:8080` (internal)
- `http://keycloak:8080` (internal)
- `postgres:5432`
- `rabbitmq:5672`
- `redis:6379`





