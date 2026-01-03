/**
 * Achievements Bounded Context
 * 
 * Context Mapping:
 * - Uses ACL (Anti-Corruption Layer) bounded context to consume Game context events
 * - Listens to external game service events via RabbitMQ (through ACL)
 * - Publishes achievement unlocked events via shared events
 * 
 * Dependencies:
 * - shared: Infrastructure, shared events, and messaging configuration
 * - acl: Anti-Corruption Layer for cross-context communication
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "acl"}
)
@org.springframework.modulith.NamedInterface("Achievements")
package com.banditgames.platform.achievements;

