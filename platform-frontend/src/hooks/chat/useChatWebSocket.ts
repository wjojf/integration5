import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useKeycloak } from "@react-keycloak/web";
import { toast } from "sonner";

import {Message, MessageStatus} from "../../types/app.types";
import type { PageableResponse, WebSocketMessageNotification } from "../../types/api.types";
import { QUERY_KEYS } from "../../config/api.config";
import { ChatWsClient } from "../../lib/app/chat.ws.client";

const { CHAT } = QUERY_KEYS;

type UseChatWebSocketOptions = { 
  appendToCache?: boolean; 
  debug?: boolean;
  showToastNotifications?: boolean;
  getSenderUsername?: (senderId: string) => string | undefined;
}

export function useChatWebSocket(options: UseChatWebSocketOptions = {}) {
  const queryClient = useQueryClient();
  const { keycloak } = useKeycloak();
  const clientRef = useRef<ChatWsClient | null>(null);
  const [connected, setConnected] = useState(false);

  const appendToCache = options.appendToCache ?? true;
  const debug = options.debug ?? false;
  const showToastNotifications = options.showToastNotifications ?? false;
  const getSenderUsername = options.getSenderUsername;

  const onMessage = useMemo(
      () => (notification: WebSocketMessageNotification) => {
        const friendId = notification.senderId;
        // messageId can be UUID (from backend) or string, ensure it's a string
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
        }

        // Show toast notification if enabled
        if (showToastNotifications) {
          const senderUsername = getSenderUsername 
            ? getSenderUsername(notification.senderId)
            : undefined;
          const displayName = senderUsername || notification.senderId;
          
          toast.info(`New message from ${displayName}`, {
            description: notification.content.length > 50 
              ? notification.content.substring(0, 50) + '...'
              : notification.content,
          });
        }

        if (appendToCache) {
          queryClient.setQueriesData(
              { queryKey: CHAT.CONVERSATION(friendId) },
              (old: PageableResponse<Message> | undefined) => {
                if (!old) return old;

                const existing = old.content ?? [];
                const existingMessage = existing.some((item) => item.id === message.id);
                if (existingMessage) return old;

                return { ...old, content: [...existing, message] };
              }
          );
        } else {
          queryClient.invalidateQueries({ queryKey: CHAT.CONVERSATION(friendId) });
        }
      },
      [appendToCache, queryClient, showToastNotifications, getSenderUsername]
  );

  useEffect(() => {
    // Only connect if keycloak is initialized and authenticated
    if (!keycloak || !keycloak.authenticated) {
      if (debug) {
        console.log("[ChatWS] Skipping connection - not authenticated");
      }
      return;
    }

    const ws = new ChatWsClient({ 
      onMessage, 
      onConnectionChange: setConnected, 
      debug,
      keycloak 
    });

    ws.connect();
    clientRef.current = ws;

    return () => {
      void clientRef.current?.disconnect();
      clientRef.current = null;
    };
  }, [onMessage, keycloak, debug]);

  // Handle token refresh - reconnect when token changes
  useEffect(() => {
    if (keycloak?.token && clientRef.current) {
      // If not connected, try to connect
      if (!clientRef.current.isConnected()) {
        if (debug) {
          console.log("[ChatWS] Token available, connecting...");
        }
        clientRef.current.connect();
      }
      // If already connected but token changed, we might need to reconnect
      // For now, STOMP will handle reconnection automatically
    }
  }, [keycloak?.token, debug]);

  return {
    connected,
    client: clientRef.current
  };
}
