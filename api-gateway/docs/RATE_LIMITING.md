# Redis Rate Limiting Implementation

## Overview

The API Gateway implements **Redis-based rate limiting** using Spring Cloud Gateway's `RequestRateLimiter` filter with a token bucket algorithm. This provides distributed rate limiting that works across multiple gateway instances.

## Architecture

### Components

1. **Redis**: Stores rate limit state (tokens, timestamps) for each user/IP
2. **RedisRateLimiterConfig**: Configures reactive Redis template and validates connection
3. **RateLimitConfig**: Provides key resolver (user ID or IP address)
4. **RequestRateLimiter Filter**: Enforces rate limits per route

### Token Bucket Algorithm

The rate limiter uses a token bucket algorithm:
- **replenishRate**: Number of tokens added per second
- **burstCapacity**: Maximum number of tokens (burst allowance)
- **requestedTokens**: Tokens consumed per request (typically 1)

Example: `replenishRate: 100, burstCapacity: 200` means:
- 100 requests per second sustained rate
- Up to 200 requests in a burst
- After burst, tokens refill at 100/second

## Configuration

### Redis Connection

Configured in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Rate Limit Configuration

Rate limits are configured per route in `application.yml`:

```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 100
    redis-rate-limiter.burstCapacity: 200
    redis-rate-limiter.requestedTokens: 1
    key-resolver: "#{@userKeyResolver}"
```

### Current Rate Limits

| Route | Replenish Rate | Burst Capacity | Description |
|-------|---------------|----------------|-------------|
| Platform Backend | 100/sec | 200 | General platform API |
| Game Service | 200/sec | 400 | Game operations |
| Chess Game Sessions | 50/sec | 100 | Chess game creation/status |
| Chess Game Moves | 100/sec | 200 | Chess move operations |
| Chess Registration | 10/sec | 20 | Game registration (lower to prevent abuse) |
| External Games (Generic) | 50/sec | 100 | Other external game APIs |

## Key Resolution

The `userKeyResolver` bean resolves rate limit keys based on:

1. **Authenticated Users**: Uses JWT `sub` claim (user ID)
2. **Anonymous Users**: Falls back to IP address
3. **Unknown**: Uses "anonymous" key

This ensures:
- Each authenticated user has their own rate limit bucket
- Anonymous users are rate limited by IP
- Rate limits are consistent across gateway instances

## Implementation Details

### RedisRateLimiterConfig

- Configures `ReactiveRedisTemplate` for rate limiting operations
- Validates Redis connection on startup
- Uses string serialization for keys and values

### Rate Limit Keys in Redis

Redis stores rate limit state with keys like:
- `request_rate_limiter.{user_id}.tokens` - Current token count
- `request_rate_limiter.{user_id}.timestamp` - Last update timestamp

### Response Headers

When rate limited, the gateway returns:
- **HTTP 429 Too Many Requests**
- `X-RateLimit-Remaining`: Remaining tokens
- `X-RateLimit-Replenish-Rate`: Tokens per second
- `X-RateLimit-Burst-Capacity`: Maximum burst capacity

## Monitoring

### Health Check

Redis connection is validated on startup. Check logs for:
```
✓ Redis connection verified for rate limiting
```

### Metrics

Rate limiting metrics are available via Actuator:
- `/actuator/metrics/gateway.requests` - Request counts
- `/actuator/gateway/routes` - Route configuration

### Redis Inspection

To inspect rate limit state in Redis:

```bash
# Connect to Redis
redis-cli

# List rate limit keys
KEYS request_rate_limiter.*

# Check token count for a user
GET request_rate_limiter.{user_id}.tokens

# Check timestamp
GET request_rate_limiter.{user_id}.timestamp
```

## Testing

### Test Rate Limiting

1. **Authenticated User**:
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/platform/lobbies
   ```

2. **Anonymous User**:
   ```bash
   curl http://localhost:8080/api/platform/lobbies
   ```

3. **Exceed Rate Limit**:
   ```bash
   # Send requests faster than replenishRate
   for i in {1..250}; do
     curl -H "Authorization: Bearer <token>" \
          http://localhost:8080/api/platform/lobbies
   done
   # Should see 429 after ~200 requests (burstCapacity)
   ```

### Verify Redis Usage

```bash
# Monitor Redis commands
redis-cli MONITOR

# Check memory usage
redis-cli INFO memory
```

## Troubleshooting

### Rate Limiting Not Working

1. **Check Redis Connection**:
   - Verify Redis is running: `docker ps | grep redis`
   - Check connection logs in gateway startup
   - Test connection: `redis-cli ping`

2. **Verify Configuration**:
   - Ensure `redis-rate-limiter.*` args are present in route filters
   - Check `key-resolver` bean is configured

3. **Check Redis Keys**:
   - Verify keys are being created: `KEYS request_rate_limiter.*`
   - Check token values are updating

### High Memory Usage

Rate limit keys have TTL (time-to-live) based on `fill_time * 2`:
- Keys expire automatically after inactivity
- Old keys are cleaned up by Redis

### Distributed Rate Limiting

When running multiple gateway instances:
- All instances share the same Redis instance
- Rate limits are enforced globally across all instances
- Token bucket state is synchronized via Redis

## Best Practices

1. **Set Appropriate Limits**:
   - Higher limits for authenticated users
   - Lower limits for anonymous/public endpoints
   - Consider endpoint criticality

2. **Monitor Rate Limit Hits**:
   - Track 429 responses
   - Adjust limits based on actual usage patterns
   - Alert on sustained rate limit violations

3. **Redis Performance**:
   - Use Redis persistence for production
   - Monitor Redis memory usage
   - Consider Redis cluster for high availability

4. **Key Design**:
   - User-based keys for authenticated endpoints
   - IP-based keys for public endpoints
   - Consider rate limiting by endpoint path

## Future Enhancements

- [ ] Per-endpoint rate limit configuration
- [ ] Rate limit bypass for admin users
- [ ] Rate limit metrics dashboard
- [ ] Dynamic rate limit adjustment
- [ ] Rate limit whitelist/blacklist

