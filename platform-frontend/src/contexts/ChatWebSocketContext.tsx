import { createContext, useContext, useEffect, useRef, useState, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useKeycloak } from "@react-keycloak/web";
import { toast } from "sonner";

import { Message, MessageStatus } from "../types/app.types";
import type { PageableResponse, WebSocketMessageNotification } from "../types/api.types";
import { QUERY_KEYS } from "../config/api.config";
import { ChatWsClient } from "../lib/app/chat.ws.client";
import { useFriends } from "../hooks/friend/useFriends";

const { CHAT } = QUERY_KEYS;

interface ChatWebSocketContextValue {
  connected: boolean;
  client: ChatWsClient | null;
}

const ChatWebSocketContext = createContext<ChatWebSocketContextValue | null>(null);

/**
 * Provider component that maintains a single global WebSocket connection for chat.
 * This ensures only one connection is active and provides toast notifications.
 */
export function ChatWebSocketProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const { keycloak, initialized } = useKeycloak();
  const { data: friends = [] } = useFriends();
  const clientRef = useRef<ChatWsClient | null>(null);
  const [connected, setConnected] = useState(false);

  // Log when provider mounts
  useEffect(() => {
    console.log('[ChatWebSocketProvider] Provider mounted/initialized');
    return () => {
      console.log('[ChatWebSocketProvider] Provider unmounting');
    };
  }, []);

  // Use refs to keep callbacks current without recreating the client
  const friendsRef = useRef(friends);
  const queryClientRef = useRef(queryClient);

  useEffect(() => {
    friendsRef.current = friends;
  }, [friends]);

  useEffect(() => {
    queryClientRef.current = queryClient;
  }, [queryClient]);

  // Message handler with toast notifications
  // Use refs so the callback always has the latest data without recreating the client
  const onMessage = useMemo(
    () => (notification: WebSocketMessageNotification) => {
      const friendId = notification.senderId;
      const messageId = typeof notification.messageId === 'string' 
        ? notification.messageId 
        : String(notification.messageId);
      
      const message: Message = {
        id: messageId,
        senderId: notification.senderId,
        receiverId: notification.receiverId,
        content: notification.content,
        status: notification.status as MessageStatus,
        sentAt: notification.sentAt,
      };

      // Get sender username from current friends list
      const currentFriends = friendsRef.current;
      const friend = currentFriends.find((f) => f.playerId === notification.senderId);
      const senderUsername = friend?.username;
      const displayName = senderUsername || notification.senderId;
      
      // Show toast notification
      toast.info(`New message from ${displayName}`, {
        description: notification.content.length > 50 
          ? notification.content.substring(0, 50) + '...'
          : notification.content,
      });

      // Update cache using current queryClient
      // Messages are sorted by sentAt descending (most recent first)
      queryClientRef.current.setQueriesData(
        { queryKey: CHAT.CONVERSATION(friendId) },
        (old: PageableResponse<Message> | undefined) => {
          if (!old) {
            // If no cache exists, create a minimal one
            return {
              content: [message],
              totalElements: 1,
              totalPages: 1,
              size: 50,
              number: 0,
              first: true,
              last: true,
              numberOfElements: 1,
              empty: false,
              pageable: {
                pageNumber: 0,
                pageSize: 50,
                sort: { sorted: false, empty: true },
                offset: 0,
                paged: true,
                unpaged: false
              },
              sort: { sorted: false, empty: true }
            };
          }

          const existing = old.content ?? [];
          const existingMessage = existing.some((item) => item.id === message.id);
          if (existingMessage) return old;

          // Add message to the beginning (most recent first)
          return { 
            ...old, 
            content: [message, ...existing],
            totalElements: old.totalElements + 1,
            numberOfElements: old.numberOfElements + 1
          };
        }
      );
      
      // Also invalidate to ensure UI updates
      queryClientRef.current.invalidateQueries({ queryKey: CHAT.CONVERSATION(friendId) });
    },
    [] // Empty deps - we use refs for friends and queryClient
  );

  useEffect(() => {
    console.log('[ChatWebSocketProvider] Effect triggered', {
      initialized,
      authenticated: keycloak?.authenticated,
      hasToken: !!keycloak?.token,
      hasClient: !!clientRef.current
    });

    // Only connect if keycloak is initialized and authenticated
    if (!initialized || !keycloak?.authenticated) {
      console.log('[ChatWebSocketProvider] Skipping connection - not initialized or not authenticated');
      if (clientRef.current) {
        console.log('[ChatWebSocketProvider] Disconnecting existing client');
        void clientRef.current.disconnect();
        clientRef.current = null;
        setConnected(false);
      }
      return;
    }

    // Create connection if it doesn't exist
    if (!clientRef.current) {
      console.log('[ChatWebSocketProvider] Creating new WebSocket client');
      const ws = new ChatWsClient({ 
        onMessage, 
        onConnectionChange: (connected) => {
          console.log('[ChatWebSocketProvider] Connection state changed:', connected);
          setConnected(connected);
        }, 
        debug: true, // Enable debug logging
        keycloak 
      });

      console.log('[ChatWebSocketProvider] Attempting to connect...');
      ws.connect();
      clientRef.current = ws;
    } else {
      console.log('[ChatWebSocketProvider] Client already exists, checking connection status');
      if (!clientRef.current.isConnected()) {
        console.log('[ChatWebSocketProvider] Client exists but not connected, reconnecting...');
        clientRef.current.connect();
      }
    }

    return () => {
      // Don't disconnect on unmount - keep connection alive
      // Only disconnect when keycloak auth changes
      console.log('[ChatWebSocketProvider] Cleanup - keeping connection alive');
    };
  }, [initialized, keycloak?.authenticated, keycloak, onMessage]);

  // Handle token refresh - reconnect when token changes
  useEffect(() => {
    if (keycloak?.token && clientRef.current) {
      console.log('[ChatWebSocketProvider] Token available, checking connection status');
      // If not connected, try to connect
      if (!clientRef.current.isConnected()) {
        console.log('[ChatWebSocketProvider] Not connected, attempting to connect with new token');
        clientRef.current.connect();
      } else {
        console.log('[ChatWebSocketProvider] Already connected');
      }
    } else if (!keycloak?.token) {
      console.log('[ChatWebSocketProvider] No token available');
    }
  }, [keycloak?.token]);

  const value = useMemo(
    () => ({
      connected,
      client: clientRef.current,
    }),
    [connected]
  );

  return (
    <ChatWebSocketContext.Provider value={value}>
      {children}
    </ChatWebSocketContext.Provider>
  );
}

/**
 * Hook to access the global chat WebSocket connection.
 * Use this instead of useChatWebSocket for components that just need connection status.
 * Returns default values if used outside the provider.
 */
export function useChatWebSocketContext() {
  const context = useContext(ChatWebSocketContext);
  if (!context) {
    // Return default values if used outside provider (shouldn't happen in normal usage)
    return {
      connected: false,
      client: null,
    };
  }
  return context;
}

