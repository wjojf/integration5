# BanditGames Platform Backend

A Spring Boot application implementing a gaming platform backend using hexagonal architecture and Spring Modulith.

## Architecture

The application follows **hexagonal architecture** (ports and adapters) with **Spring Modulith** for modular structure:

- **Domain Layer**: Core business entities and domain logic
- **Adapter Layer**: External integrations (persistence, APIs, etc.)

## Modules

### 1. Games Module
- Registering and finding games on the platform
- User favorite games management

### 2. Friends Module
- Friend request system
- Friendship status management (PENDING, ACCEPTED, REJECTED, BLOCKED)

### 3. Chat Module
- Direct messaging between users
- Message status tracking (SENT, DELIVERED, READ)

### 4. Achievements Module
- Game achievements definition
- User achievement tracking with unlock dates

### 5. Lobby Module
- Creating and joining game lobbies
- Lobby status management (WAITING, READY, STARTED, CANCELLED)

## Technology Stack

- **Spring Boot 3.2.0**
- **Spring Modulith** - Modular monolith architecture
- **Spring Data JPA** - Database persistence
- **PostgreSQL** - Database
- **Keycloak** - OAuth2/OIDC authentication and authorization
- **Gradle** - Build tool

## Setup

### Prerequisites

- Java 17+
- PostgreSQL
- Keycloak (for authentication)

### Configuration

1. Configure database connection in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/banditgames
    username: postgres
    password: postgres
```

2. Configure Keycloak in `application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/banditgames
```

### Running the Application

#### Local Development

```bash
./gradlew bootRun
```

#### Using Docker

Build and run with Docker Compose (includes PostgreSQL):

```bash
docker-compose up --build
```

Or build the Docker image separately:

```bash
docker build -t banditgames-backend .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/banditgames \
  -e KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/banditgames \
  banditgames-backend
```

## Database Schema

The application uses JPA with automatic schema generation (`ddl-auto: update`). The following tables are created:

- `games` - Game definitions
- `user_favorite_games` - User favorite games
- `friendships` - Friend relationships
- `messages` - Chat messages
- `achievements` - Achievement definitions
- `user_achievements` - User unlocked achievements
- `lobbies` - Game lobbies
- `lobby_players` - Lobby player associations

## Security

The application uses Keycloak JWT tokens for authentication. All endpoints (except `/actuator/**` and `/error`) require a valid JWT token.

## Project Structure

```
src/main/java/com/banditgames/platform/
├── app/                # Spring Boot application configuration
│   ├── config/         # Security and other configurations
│   └── PlatformBackendApplication.java
├── games/              # Games module
│   ├── domain/         # Domain entities
│   └── adapter/        # Adapters (persistence, etc.)
├── friends/            # Friends module
├── chat/               # Chat module
├── achievements/       # Achievements module
└── lobby/              # Lobby module
```

