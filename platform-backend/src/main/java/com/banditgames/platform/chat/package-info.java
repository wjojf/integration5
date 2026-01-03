/**
 * Chat Bounded Context
 * 
 * Context Mapping:
 * - Independent context with no direct dependencies on other contexts
 * - Publishes MessageSentEvent for real-time notifications
 * - Uses shared infrastructure for WebSocket and event publishing
 * 
 * Dependencies:
 * - shared: Infrastructure, shared events, and WebSocket configuration
 * 
 * Context Relationships:
 * - Published Language: Publishes MessageSentEvent for real-time messaging
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared"}
)
@org.springframework.modulith.NamedInterface("Chat")
package com.banditgames.platform.chat;

