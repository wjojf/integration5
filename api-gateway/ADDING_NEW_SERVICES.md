# Adding New Services to the Gateway

This guide explains how to add new services to the API Gateway following best practices.

## Architecture Overview

The gateway follows a **configuration-driven, full-proxy** architecture:

- **Default Behavior**: Full proxy - status codes and response bodies are passed through unchanged
- **Service Registry**: Centralized service name resolution (no hardcoding)
- **Filter Chain**: Composable filters that can be enabled/disabled per route
- **Scalability**: Easy to add 10+ services without code changes

## Adding a New Service

### Step 1: Add Route Configuration

Add your service route to `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my-new-service
          uri: ${MY_NEW_SERVICE_URL:http://localhost:9000}
          predicates:
            - Path=/api/my-service/**
          filters:
            - StripPrefix=2
            - PrefixPath=/api
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@userKeyResolver}"
            - name: CircuitBreaker
              args:
                name: myNewService
                fallbackUri: forward:/fallback/my-service
            - name: AddRequestHeader
              args:
                name: X-API-Key
                value: ${SERVICE_API_KEY}
```

### Step 2: Register Service in Service Registry

Add service mappings to `application.yml`:

```yaml
gateway:
  services:
    # Route ID to service name (exact match)
    route-id-to-service-name:
      my-new-service: my-new-service
    
    # Path prefix to service name (fallback)
    path-prefix-to-service-name:
      /api/my-service: my-new-service
```

**Note**: The service registry will automatically use these mappings. Default mappings are provided, but you can override them via configuration.

### Step 3: (Optional) Add Circuit Breaker Configuration

If you added a circuit breaker, configure it in `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      myNewService:
        baseConfig: default
```

### Step 4: (Optional) Add Fallback Endpoint

If you added a fallback, create a fallback endpoint in `FallbackController`:

```java
@GetMapping("/fallback/my-service")
public Mono<ResponseEntity<Map<String, Object>>> myServiceFallback() {
    return Mono.just(ResponseEntity.ok(
        createFallbackResponse("my-new-service", "Service temporarily unavailable")
    ));
}
```

## Service Name Resolution

The gateway uses a **ServiceResolver** that tries multiple strategies:

1. **Route ID** (exact match) - Highest priority
2. **Route ID** (prefix match) - For dynamic route IDs
3. **Path prefix** - Fallback when route ID not available
4. **Route ID as-is** - Last resort
5. **"unknown"** - If nothing matches

This means you don't need to hardcode service names in filters!

## Filter Behavior

### Default: Full Proxy

By default, all filters follow **full proxy behavior**:
- ✅ Status codes are passed through unchanged
- ✅ Response bodies are passed through unchanged
- ✅ Headers are preserved (except those explicitly modified)

### Custom Filters

If you need custom behavior (e.g., ACL transformation), create a filter:

```java
@Component
public class MyCustomFilter extends AbstractGatewayFilterFactory<MyCustomFilter.Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        if (!config.isEnabled()) {
            // Default: Full proxy
            return (exchange, chain) -> chain.filter(exchange);
        }
        
        return (exchange, chain) -> {
            // Your custom logic here
            // Remember: Default behavior should be full proxy!
            return chain.filter(exchange);
        };
    }
}
```

## Best Practices

1. **Use Service Registry**: Always register services in the service registry, don't hardcode names
2. **Full Proxy by Default**: Filters should pass through unchanged unless explicitly transforming
3. **Configuration-Driven**: Use `application.yml` for routing, not code
4. **Filter Ordering**: Use `@Order` annotation to control filter execution order
5. **Error Handling**: Use `ServiceErrorResponseFilter` for standardized error responses
6. **Logging**: Add debug logging to understand filter behavior

## Example: Adding 10 Services

To add 10 services, you would:

1. Add 10 route configurations to `application.yml`
2. Add 10 service mappings to `gateway.services` section
3. (Optional) Add circuit breaker configurations
4. (Optional) Add fallback endpoints

**No code changes needed!** The gateway will automatically:
- Resolve service names
- Handle errors consistently
- Proxy requests correctly

## Troubleshooting

### Service name not resolving?

1. Check `application.yml` has the service registered
2. Check route ID matches the mapping
3. Check path prefix matches (if using path-based resolution)
4. Enable debug logging: `logging.level.com.banditgames.gateway.service: DEBUG`

### Response not being proxied correctly?

1. Check filter order - ensure `BaseProxyFilter` runs first
2. Check if any filter is consuming the response body
3. Verify `ServerHttpResponseDecorator` is properly forwarding

### 500 errors not being intercepted?

1. Check `ServiceErrorResponseFilter` order (should be -1)
2. Verify service name is resolving correctly
3. Check logs for "Intercepted 500 error" messages
