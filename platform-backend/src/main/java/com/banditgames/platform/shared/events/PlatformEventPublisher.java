package com.banditgames.platform.shared.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Shared event publisher for cross-domain events.
 * Allows different modules to publish events that can be consumed by other modules.
 */
@Component
@Primary
public class PlatformEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public PlatformEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Object event) {
        eventPublisher.publishEvent(event);
    }
}
