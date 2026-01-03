package com.banditgames.gateway.filter;

import com.banditgames.gateway.service.ServiceResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Global filter that intercepts 500 errors from proxied services
 * and returns a standardized error response.
 * 
 * Default behavior: Full proxy - status codes and response bodies are passed through as-is.
 * Only 500 errors are intercepted and transformed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceErrorResponseFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -1; // Run early, after response is received
    private final ObjectMapper objectMapper;
    private final ServiceResolver serviceResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                // Check if response status is 500
                if (originalResponse.getStatusCode() != null && 
                    originalResponse.getStatusCode().value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                    
                    // Determine service name using service resolver
                    String serviceName = serviceResolver.resolveServiceName(exchange);
                    
                    // Create custom error response
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "500 from service " + serviceName);
                    
                    try {
                        byte[] errorBytes = objectMapper.writeValueAsBytes(errorResponse);
                        DataBuffer buffer = originalResponse.bufferFactory().wrap(errorBytes);
                        
                        // Set Content-Type header
                        originalResponse.getHeaders().set("Content-Type", "application/json");
                        
                        log.warn("Intercepted 500 error from service: {}, path: {}", 
                                serviceName, exchange.getRequest().getURI().getPath());
                        
                        // Consume original body to avoid memory leaks, then write our custom response
                        if (body instanceof Flux) {
                            Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
                            // Release each buffer in the flux and then write our custom response
                            return fluxBody
                                .doOnNext(DataBufferUtils::release)
                                .then()
                                .then(super.writeWith(Flux.just(buffer)));
                        } else {
                            // For non-Flux publishers, just write our custom response
                            return super.writeWith(Flux.just(buffer));
                        }
                    } catch (Exception e) {
                        log.error("Failed to create error response", e);
                        // Fallback: return original response
                        return super.writeWith(body);
                    }
                }
                
                // For non-500 responses, pass through as-is
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(responseDecorator).build());
    }


    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }
}
