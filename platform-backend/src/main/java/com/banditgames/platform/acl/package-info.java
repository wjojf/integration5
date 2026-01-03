/**
 * ACL (Anti-Corruption Layer) Bounded Context
 * 
 * This bounded context acts as a translation layer between other bounded contexts,
 * ensuring loose coupling and protecting contexts from each other's changes.
 * 
 * Context Mapping:
 * - Provides ACL adapters for cross-context communication
 * - Translates between different context domain models
 * - Handles external service message translation
 * 
 * Dependencies:
 * - shared: Infrastructure and shared events
 * - player: For Player context translation
 * - achievements: For Achievement context translation
 * - lobby: For Lobby context translation (indirect)
 * 
 * ACL Adapters:
 * - PlayerContextAdapter: Translates Player context for Lobby context
 * - GameContextAdapter: Translates Game context for Achievements context
 * - GameEventsAdapter: Translates external RabbitMQ messages from game service
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "player", "achievements", "lobby"}
)
@org.springframework.modulith.NamedInterface("ACL")
package com.banditgames.platform.acl;

