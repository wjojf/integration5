# BanditGames Platform - Docker Compose Setup

This document describes how to run the entire BanditGames platform using Docker Compose.

## Overview

The global `docker-compose.yml` file orchestrates all services and shared infrastructure:

- **Shared Infrastructure**: PostgreSQL, RabbitMQ, Redis, Keycloak
- **Application Services**: API Gateway, Platform Backend, Game Service
- **External Services**: Chess Game (Backend + Frontend)
- **Frontend**: Platform Frontend
- **Optional Services**: MLFlow, MinIO (for ML/AI features)

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available
- Ports 8080, 8081, 8000, 5173, 8180, 5432, 5672, 6379 available

## Quick Start

### 1. Create Environment File

```bash
cp .env.example .env
# Edit .env with your configuration if needed
```

### 2. Start All Services

```bash
# Start all core services
docker-compose up -d

# Or start with external chess game
docker-compose --profile chess up -d

# Or start with ML/AI services
docker-compose --profile ml up -d

# Or start everything
docker-compose --profile chess --profile ml up -d
```

### 3. Verify Services

```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Check specific service logs
docker-compose logs -f api-gateway
docker-compose logs -f platform-backend
docker-compose logs -f game-service
```

### 4. Access Services

- **API Gateway**: http://localhost:8080
- **Platform Backend**: http://localhost:8081
- **Game Service**: http://localhost:8000
- **Platform Frontend**: http://localhost:5173
- **Keycloak**: http://localhost:8180 (admin/admin)
- **RabbitMQ Management**: http://localhost:15672 (admin/admin)
- **Chess Game Backend**: http://localhost:8090 (if --profile chess, internal: 8080)
- **Chess Game Frontend**: http://localhost:3333 (if --profile chess)
- **MLFlow**: http://localhost:5000 (if --profile ml)
- **MinIO Console**: http://localhost:9001 (if --profile ml)

## Service Details

### Shared Infrastructure

#### PostgreSQL
- **Port**: 5432
- **Databases**: 
  - `banditgames` (Platform Backend)
  - `game_service` (Game Service)
  - `keycloak` (Keycloak)
  - `chess_game` (External Chess Game)
- **Credentials**: postgres/postgres (default)

#### RabbitMQ
- **AMQP Port**: 5672
- **Management UI**: http://localhost:15672
- **Credentials**: admin/admin (default)
- **Exchange**: `game_events` (TopicExchange)

#### Redis
- **Port**: 6379
- **Used by**: API Gateway (rate limiting, caching)

#### Keycloak
- **Port**: 8180
- **Admin Console**: http://localhost:8180
- **Credentials**: admin/admin (default)
- **Realm**: banditgames

### Application Services

#### API Gateway
- **Port**: 8080
- **Health**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/api/docs/swagger-ui.html
- **Dependencies**: Keycloak, Redis, Platform Backend, Game Service

#### Platform Backend
- **Port**: 8081
- **Health**: http://localhost:8081/actuator/health
- **Dependencies**: PostgreSQL, RabbitMQ, Game Service

#### Game Service
- **Port**: 8000
- **Health**: http://localhost:8000/health
- **OpenAPI**: http://localhost:8000/openapi.json
- **Dependencies**: PostgreSQL, RabbitMQ

#### External Chess Game
- **Backend Port**: 8090 (external), 8080 (internal container)
- **Frontend Port**: 3333
- **Image**: Pre-built from GitLab registry (`registry.gitlab.com/kdg-ti/integration-5/chessgame/back:latest` and `front:latest`)
- **Database**: Uses shared PostgreSQL `postgres` database (not separate `chess_game` database)
- **Dependencies**: PostgreSQL, RabbitMQ
- **Note**: Only starts with `--profile chess`

## Profiles

### Core Services (Default)
All essential services start by default:
- API Gateway
- Platform Backend
- Game Service
- Platform Frontend
- Shared Infrastructure

### Chess Profile (`--profile chess`)
Starts the external chess game services:
- Chess Game Backend
- Chess Game Frontend

### ML Profile (`--profile ml`)
Starts ML/AI related services:
- MLFlow (experiment tracking)
- MinIO (object storage for DVC)

## Environment Variables

Key environment variables (see `.env.example` for full list):

- `POSTGRES_USER` / `POSTGRES_PASSWORD`: Database credentials
- `RABBITMQ_USER` / `RABBITMQ_PASSWORD`: RabbitMQ credentials
- `SERVICE_API_KEY`: Service-to-service authentication key
- `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`: Keycloak admin credentials
- `CHESS_GAME_BACKEND_PATH`: Path to chess game backend (if external)
- `CHESS_GAME_FRONTEND_PATH`: Path to chess game frontend (if external)

## Database Initialization

The PostgreSQL container automatically:
1. Creates multiple databases (banditgames, game_service, keycloak, chess_game)
2. Runs migrations from `platform-backend/src/main/resources/db/migration`
3. Initializes game-service schema from `game-service/docs/database/schema.sql`

## Network

All services are connected to the `banditgames-network` bridge network, allowing them to communicate using service names (e.g., `http://platform-backend:8081`).

## Health Checks

All services include health checks. Use `docker-compose ps` to see health status.

## Troubleshooting

### Services Not Starting

1. Check logs: `docker-compose logs <service-name>`
2. Verify ports are not in use: `netstat -an | grep <port>`
3. Check health status: `docker-compose ps`
4. Verify environment variables: `docker-compose config`

### Database Connection Issues

1. Ensure PostgreSQL is healthy: `docker-compose ps postgres`
2. Check database exists: `docker-compose exec postgres psql -U postgres -l`
3. Verify connection string in service logs

### RabbitMQ Connection Issues

1. Check RabbitMQ is healthy: `docker-compose ps rabbitmq`
2. Access management UI: http://localhost:15672
3. Verify credentials match in all services

### Port Conflicts

If ports are already in use, update them in `.env`:
```bash
API_GATEWAY_PORT=8080
PLATFORM_BACKEND_PORT=8081
# etc.
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes data)
docker-compose down -v

# Stop specific service
docker-compose stop <service-name>
```

## Rebuilding Services

```bash
# Rebuild all services
docker-compose build

# Rebuild specific service
docker-compose build <service-name>

# Rebuild and restart
docker-compose up -d --build <service-name>
```

## Development Mode

For development, you may want to:
1. Mount source code as volumes for hot reload
2. Use development profiles
3. Enable debug logging

See individual service READMEs for development setup.

## Production Considerations

For production deployment:
1. Use strong passwords (update `.env`)
2. Enable SSL/TLS
3. Configure proper resource limits
4. Set up monitoring and logging
5. Use external managed databases if needed
6. Configure backup strategies

## External Chess Game

If the chess game is in a separate repository:

1. Clone the chess game repository
2. Update `.env`:
   ```bash
   CHESS_GAME_BACKEND_PATH=../external-chess-game/backend
   CHESS_GAME_FRONTEND_PATH=../external-chess-game/frontend
   ```
3. Start with chess profile: `docker-compose --profile chess up -d`

## Support

For issues or questions:
1. Check service logs: `docker-compose logs <service>`
2. Review architecture documentation: `docs/ARCHITECTURE.md`
3. Check individual service READMEs



