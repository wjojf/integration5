/**
 * Lobby Bounded Context
 * 
 * Context Mapping:
 * - Uses ACL (Anti-Corruption Layer) bounded context to access Player context
 * - Listens to Game context events via shared events
 * - Publishes lobby events that can be consumed by other contexts
 * 
 * Dependencies:
 * - shared: Infrastructure and shared events
 * - acl: Anti-Corruption Layer for cross-context communication
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "acl"}
)
@org.springframework.modulith.NamedInterface("Lobby")
package com.banditgames.platform.lobby;

