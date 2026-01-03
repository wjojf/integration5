# BanditGames API Gateway

Centralized API Gateway for the BanditGames platform. Provides a single entry point for all client requests with authentication, routing, rate limiting, and service-to-service communication.

## Features

- **Authentication & Authorization**: JWT validation via Keycloak (SSO)
- **Request Routing**: Routes requests to appropriate backend services
- **Rate Limiting**: Per-user and per-endpoint rate limiting using Redis
- **Circuit Breaking**: Resilience4j circuit breakers for fault tolerance
- **ACL (Anti-Corruption Layer)**: Transforms external game events to platform format
- **Service-to-Service Auth**: API key-based authentication for internal services
- **Swagger/OpenAPI**: Aggregated API documentation from all services
- **Request Logging**: Comprehensive request/response logging for monitoring
- **CORS Support**: Configurable CORS for frontend integration

## Architecture

```
Frontend (React)
    ↓
API Gateway (Port 8080)
    ├── Authentication (Keycloak JWT)
    ├── Rate Limiting
    ├── Circuit Breaking
    ├── Request Routing
    └── ACL Transformation
    ↓
Backend Services:
    ├── Platform Backend (Port 8081)
    └── Game Service (Port 8000)
```

## API Routes

### Platform Backend Routes
- `/api/platform/**` → Routes to `platform-backend:8081`
  - Lobbies, friends, achievements, game registry

### Game Service Routes
- `/api/games/games/**` → Game engine endpoints
- `/api/games/sessions/**` → Game session management
- `/api/games/chatbot/**` → Chatbot queries
- `/api/games/ai-player/**` → AI player endpoints
- `/api/games/ml/**` → ML model endpoints
- `/api/games/game-logger/**` → Game logging endpoints
- `/api/games/health` → Health check
- `/api/games/docs/**` → Swagger documentation

### External Games Routes
- `/api/external/**` → External game integration (ACL transformation)

## Configuration

### Environment Variables

```bash
# Service URLs
PLATFORM_BACKEND_URL=http://platform-backend:8081
GAME_SERVICE_URL=http://game-service:8000

# Keycloak Configuration
KEYCLOAK_ISSUER_URI=http://keycloak:8090/realms/banditgames
KEYCLOAK_JWK_SET_URI=http://keycloak:8090/realms/banditgames/protocol/openid-connect/certs

# Redis Configuration (for rate limiting)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Service-to-Service Authentication
SERVICE_API_KEY=your-secret-api-key

# CORS
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

## Authentication

### Client Authentication (Frontend)

All requests (except public endpoints) require a JWT token from Keycloak:

```http
Authorization: Bearer <jwt-token>
```

The gateway validates the token and adds user information to request headers:
- `X-User-Id`: User ID from JWT
- `X-Username`: Username from JWT
- `X-User-Roles`: Comma-separated list of roles

### Service-to-Service Authentication

Internal services communicate using API keys:

```http
X-API-Key: <service-api-key>
X-Service-Name: <service-name>
```

## Rate Limiting

Rate limits are configured per route:

- **Platform Backend**: 100 requests/second, burst 200
- **Game Service - Games**: 200 requests/second, burst 400
- **Game Service - Chatbot**: 50 requests/second, burst 100
- **Game Service - AI Player**: 100 requests/second, burst 200
- **Game Service - ML Models**: 50 requests/second, burst 100
- **Game Service - Logger**: 100 requests/second, burst 200

Rate limiting is based on:
- Authenticated users: User ID from JWT
- Anonymous users: IP address

## Circuit Breaking

Circuit breakers are configured for both backend services:

- **Failure Threshold**: 50%
- **Sliding Window**: 10 requests
- **Wait Duration**: 10 seconds
- **Half-Open State**: 3 permitted calls

When a circuit is open, requests are forwarded to fallback endpoints that return `503 Service Unavailable`.

## ACL (Anti-Corruption Layer)

The ACL filter transforms external game events to platform event format:

**External Event Types → Platform Event Types:**
- `GAME_STARTED` → `GAME_START`
- `MOVE_MADE` → `GAME_MOVE_REQUEST`
- `GAME_ENDED` → `GAME_END`
- `STATE_CHANGED` → `GAME_STATE_UPDATED`
- `ACHIEVEMENT_UNLOCKED` → `ACHIEVEMENT_UNLOCKED`

## API Documentation

### Swagger UI
- URL: `http://localhost:8080/api/docs/swagger-ui.html`
- Aggregates documentation from all backend services

### OpenAPI JSON
- URL: `http://localhost:8080/api/docs/openapi.json`
- Gateway OpenAPI specification

### Aggregated Docs
- URL: `http://localhost:8080/api/docs/aggregate`
- Combined documentation from all services

## Public Endpoints

These endpoints don't require authentication:

- `/actuator/**` - Spring Boot Actuator endpoints
- `/health` - Health check
- `/fallback/**` - Circuit breaker fallback endpoints
- `/api/docs/**` - API documentation
- `/api/external/**` - External game integration (ACL handles auth)

## Building

```bash
./gradlew build
```

## Running

### Local Development
```bash
./gradlew bootRun
```

### Docker
```bash
docker build -t api-gateway .
docker run -p 8080:8080 api-gateway
```

### Docker Compose
```bash
docker-compose up api-gateway
```

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

### Gateway Routes
```bash
curl http://localhost:8080/actuator/gateway/routes
```

## Development

### Adding a New Route

1. Add route configuration to `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: http://new-service:8080
          predicates:
            - Path=/api/new/**
          filters:
            - StripPrefix=2
```

2. Update `GatewayConstants.java` if needed

3. Add rate limiting and circuit breaker configuration

### Adding a New Filter

1. Create filter class implementing `GlobalFilter` or `GatewayFilter`
2. Annotate with `@Component`
3. Implement `filter()` method
4. Set filter order using `getOrder()` method

## Troubleshooting

### Service Unavailable
- Check if backend services are running
- Check circuit breaker status: `/actuator/health`
- Review logs for connection errors

### Authentication Failures
- Verify Keycloak is running and accessible
- Check JWT token validity
- Verify `KEYCLOAK_JWK_SET_URI` configuration

### Rate Limiting Issues
- Check Redis connection
- Verify rate limit configuration
- Review rate limit logs

## Security Best Practices

1. **Never expose API keys in code** - Use environment variables
2. **Use HTTPS in production** - Configure SSL/TLS
3. **Rotate API keys regularly** - Implement key rotation
4. **Monitor authentication failures** - Set up alerts
5. **Limit CORS origins** - Only allow trusted domains
6. **Use strong rate limits** - Prevent abuse
7. **Enable circuit breakers** - Prevent cascade failures

## License

MIT License
