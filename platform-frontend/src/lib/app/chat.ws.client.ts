import { Client, StompSubscription, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import Keycloak from "keycloak-js";

import type { WebSocketMessageNotification } from "../../types/api.types";
import { API_CONFIG, API_ENDPOINTS } from "../../config/api.config";

const { WS_URL } = API_CONFIG;
const { WEBSOCKET } = API_ENDPOINTS;

type ChatWsClientOptions = {
    onMessage: (msg: WebSocketMessageNotification) => void;
    onConnectionChange?: (connected: boolean) => void;
    debug?: boolean;
    keycloak?: Keycloak;
};

export class ChatWsClient {
    private client: Client;
    private subscription: StompSubscription | null = null;
    private connected = false;
    private keycloak: Keycloak | undefined;

    constructor(private opts: ChatWsClientOptions) {
        this.keycloak = opts.keycloak;
        
        // Build WebSocket URL - SockJS expects HTTP/HTTPS URL, not ws://
        // Backend endpoint is /ws with SockJS
        const wsUrl = WS_URL.replace(/^ws/, "http").replace(/^wss/, "https");
        
        console.log("[ChatWsClient] Creating WebSocket client", {
            originalWsUrl: WS_URL,
            convertedUrl: wsUrl,
            hasKeycloak: !!opts.keycloak,
            hasToken: !!opts.keycloak?.token
        });
        
        this.client = new Client({
            // Use SockJS for WebSocket connection (backend uses SockJS)
            // webSocketFactory is called each time we need to create a connection
            webSocketFactory: () => {
                if (opts.debug) {
                    console.log("[STOMP] Creating SockJS connection to", wsUrl);
                }
                return new SockJS(wsUrl) as any;
            },
            reconnectDelay: API_CONFIG.WEBSOCKET.RECONNECT_DELAY,
            heartbeatIncoming: API_CONFIG.WEBSOCKET.HEARTBEAT_INCOMING,
            heartbeatOutgoing: API_CONFIG.WEBSOCKET.HEARTBEAT_OUTGOING,
            debug: opts.debug ? (str: string) => console.log("[STOMP]", str) : undefined,
            
            // Add JWT token to connection headers for authentication
            connectHeaders: this.getAuthHeaders(),

            onConnect: () => {
                console.log("[ChatWsClient] STOMP connected successfully!");
                this.connected = true;
                this.opts.onConnectionChange?.(true);

                // Subscribe to user-specific message queue
                // Backend sends to /user/{userId}/queue/messages via convertAndSendToUser
                // Spring automatically resolves /user/queue/messages to the authenticated user's queue
                console.log("[ChatWsClient] Subscribing to", WEBSOCKET.MESSAGES);
                this.subscription = this.client.subscribe(WEBSOCKET.MESSAGES, (frame: IMessage) => {
                    try {
                        const payload = JSON.parse(frame.body) as WebSocketMessageNotification;
                        console.log("[ChatWsClient] Received message via WebSocket:", payload);
                        if (opts.debug) {
                            console.log("[STOMP] Received message:", payload);
                        }
                        this.opts.onMessage(payload);
                    } catch (error) {
                        console.error("[STOMP] Failed to parse WebSocket message:", error, frame.body);
                    }
                });
                
                console.log("[ChatWsClient] Successfully subscribed to message queue");
                if (opts.debug) {
                    console.log("[STOMP] Connected and subscribed to", WEBSOCKET.MESSAGES);
                }
            },

            onStompError: (frame) => {
                console.error("[STOMP] STOMP Error:", frame);
                this.connected = false;
                this.opts.onConnectionChange?.(false);
                // Unsubscribe on error to allow reconnection
                if (this.subscription) {
                    this.subscription.unsubscribe();
                    this.subscription = null;
                }
            },

            onWebSocketError: (event: Event) => {
                console.error("[STOMP] WebSocket Error:", event);
                this.connected = false;
                this.opts.onConnectionChange?.(false);
                // Unsubscribe on error to allow reconnection
                if (this.subscription) {
                    this.subscription.unsubscribe();
                    this.subscription = null;
                }
            },

            onWebSocketClose: () => {
                this.connected = false;
                this.opts.onConnectionChange?.(false);
                if (opts.debug) {
                    console.log("[STOMP] WebSocket closed");
                }
            },

            onDisconnect: () => {
                this.connected = false;
                this.opts.onConnectionChange?.(false);
                if (opts.debug) {
                    console.log("[STOMP] Disconnected");
                }
            },
        });
    }

    private getAuthHeaders(): Record<string, string> {
        if (this.keycloak?.token) {
            return { Authorization: `Bearer ${this.keycloak.token}` };
        }
        return {};
    }

    connect() {
        console.log("[ChatWsClient] connect() called", {
            active: this.client.active,
            connected: this.connected,
            hasToken: !!this.keycloak?.token,
            wsUrl: WS_URL
        });

        if (this.client.active) {
            console.log("[ChatWsClient] Already active, skipping connection");
            if (this.opts.debug) {
                console.log("[STOMP] Already connected");
            }
            return;
        }
        
        // Update token if available (for reconnection with refreshed token)
        const authHeaders = this.getAuthHeaders();
        console.log("[ChatWsClient] Auth headers:", Object.keys(authHeaders).length > 0 ? "Present" : "Missing");
        
        if (Object.keys(authHeaders).length > 0) {
            this.client.configure({
                connectHeaders: authHeaders
            });
        } else {
            console.warn("[ChatWsClient] No auth headers available - connection may fail");
        }
        
        console.log("[ChatWsClient] Activating STOMP client to", WS_URL);
        if (this.opts.debug) {
            console.log("[STOMP] Connecting to", WS_URL);
        }
        
        try {
            this.client.activate();
            console.log("[ChatWsClient] Client activation initiated");
        } catch (error) {
            console.error("[ChatWsClient] Failed to activate client:", error);
            this.connected = false;
            this.opts.onConnectionChange?.(false);
        }
    }

    async disconnect() {
        this.subscription?.unsubscribe();
        this.subscription = null;
        await this.client.deactivate();
        this.connected = false;
        this.opts.onConnectionChange?.(false);
    }

    publish(destination: string, body: unknown) {
        if (!this.connected || !this.client.active) {
            console.warn("[STOMP] Cannot publish - not connected");
            return;
        }
        this.client.publish({
            destination,
            body: JSON.stringify(body),
        });
    }

    isConnected(): boolean {
        return this.connected && this.client.active;
    }
}
