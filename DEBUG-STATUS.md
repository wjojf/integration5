# Service Launch & Debug Status

## Current Status (as of launch)

### ✅ Running & Healthy Services
- **PostgreSQL** (port 5432) - Healthy
- **RabbitMQ** (ports 5672, 15672) - Healthy  
- **Redis** (port 6379) - Healthy

### ⚠️ Running but Health Checks in Progress
- **Game Service** (port 8000) - Running, health check starting
- **Platform Backend** (port 8081) - Running, health check starting
- **Keycloak** (port 8180) - Running, health check starting

### ❌ Not Started (Dependency Issues)
- **API Gateway** (port 8080) - Waiting for Keycloak to be healthy
- **Platform Frontend** (port 5173) - Waiting for API Gateway

## Issues Fixed

1. **Redis Command Interpolation** - Fixed shell variable expansion in Redis command
2. **Keycloak Volume Mounts** - Commented out optional volume mounts that don't exist
3. **Health Check Commands** - Removed `|| exit 1` from health check URLs (was being interpreted as part of URL path)
4. **Game Service Health Check** - Changed from `wget` to `curl` (curl is installed in container)

## Known Issues

1. **Health Check Timing** - Some services take 30-60 seconds to pass health checks:
   - Keycloak: Takes ~30-60 seconds to initialize database and start
   - Platform Backend: Takes ~10-15 seconds to start Spring Boot
   - Game Service: Takes ~5-10 seconds to start FastAPI

2. **API Gateway Dependencies** - API Gateway won't start until:
   - Keycloak is healthy (for JWT validation)
   - Platform Backend is healthy (for routing)
   - Game Service is healthy (for routing)

## Debugging Commands

### Check Service Status
```bash
docker-compose ps
```

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f game-service
docker-compose logs -f platform-backend
docker-compose logs -f api-gateway
docker-compose logs -f keycloak
```

### Test Service Endpoints
```bash
# Game Service
curl http://localhost:8000/health

# Platform Backend
curl http://localhost:8081/actuator/health

# Keycloak
curl http://localhost:8180/health/ready

# API Gateway (when running)
curl http://localhost:8080/actuator/health
```

### Restart Services
```bash
# Restart specific service
docker-compose restart <service-name>

# Restart all services
docker-compose restart

# Recreate and restart
docker-compose up -d --force-recreate <service-name>
```

### Check Health Check Status
```bash
# Inspect container health
docker inspect <container-name> | grep -A 10 Health
```

## Next Steps

1. **Wait for Health Checks** - Services need time to pass health checks (especially Keycloak)
2. **Start API Gateway** - Once Keycloak is healthy, API Gateway should start automatically
3. **Start Platform Frontend** - Once API Gateway is healthy, frontend can start
4. **Start Chess Services** - Use `--profile chess` to start external chess game
5. **Start ML Services** - Use `--profile ml` to start MLFlow and MinIO

## Service URLs

- **API Gateway**: http://localhost:8080
- **Platform Backend**: http://localhost:8081
- **Game Service**: http://localhost:8000
- **Platform Frontend**: http://localhost:5173
- **Keycloak**: http://localhost:8180 (admin/admin)
- **RabbitMQ Management**: http://localhost:15672 (admin/admin)
- **Chess Backend**: http://localhost:8090 (with --profile chess)
- **Chess Frontend**: http://localhost:3333 (with --profile chess)
- **MLFlow**: http://localhost:5000 (with --profile ml)
- **MinIO Console**: http://localhost:9001 (with --profile ml)

## Troubleshooting

### Service Won't Start
1. Check logs: `docker-compose logs <service-name>`
2. Check dependencies: `docker-compose ps` (ensure dependencies are healthy)
3. Check health: `docker inspect <container-name> | grep Health`

### Health Check Failing
1. Test endpoint manually: `curl http://localhost:<port>/health`
2. Check if service is actually running: `docker-compose logs <service-name>`
3. Increase health check timeout in docker-compose.yml if needed

### Port Conflicts
1. Check if port is in use: `lsof -i :<port>` or `netstat -an | grep <port>`
2. Update port in `.env` file
3. Restart services: `docker-compose down && docker-compose up -d`



