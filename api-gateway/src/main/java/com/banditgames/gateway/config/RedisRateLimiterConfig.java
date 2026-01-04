package com.banditgames.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis rate limiter configuration for Spring Cloud Gateway.
 * 
 * Spring Cloud Gateway automatically configures RedisRateLimiter when:
 * 1. Redis is configured (spring.data.redis.*)
 * 2. RequestRateLimiter filter uses redis-rate-limiter.* args
 * 
 * This configuration ensures Redis connection is properly validated and
 * provides explicit reactive Redis template configuration for rate limiting.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisRateLimiterConfig {

    /**
     * Configures reactive Redis template for rate limiting operations.
     * Spring Cloud Gateway uses this for token bucket algorithm.
     * 
     * Note: Spring Cloud Gateway will automatically create a RedisRateLimiter
     * bean when it detects this template and redis-rate-limiter configuration
     * in route filters.
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        RedisSerializationContext<String, String> serializationContext = 
            RedisSerializationContext.<String, String>newSerializationContext(RedisSerializer.string())
                .key(RedisSerializer.string())
                .value(RedisSerializer.string())
                .hashKey(RedisSerializer.string())
                .hashValue(RedisSerializer.string())
                .build();

        ReactiveRedisTemplate<String, String> template = 
            new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
        
        // Test Redis connection on startup
        template.getConnectionFactory().getReactiveConnection()
            .ping()
            .doOnSuccess(pong -> log.info("✓ Redis connection verified for rate limiting"))
            .doOnError(error -> log.error("✗ Failed to connect to Redis for rate limiting: {}", error.getMessage()))
            .subscribe();
        
        log.info("Redis rate limiter template configured");
        return template;
    }
}
