import { useEffect, useRef, useState, useCallback } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { API_CONFIG } from '../../config/api.config';

export type GameEventType = 'game.move.applied' | 'game.session.started' | 'game.session.ended';

export interface GameMoveAppliedEvent {
  session_id: string;
  move_data: any;
  player_id: string;
  game_state: any;
  current_player_id: string;
  status: string;
  winner_id?: string | null;
  timestamp?: string;
}

export interface GameSessionStartedEvent {
  session_id: string;
  game_id: string;
  game_type: string;
  player_ids: string[];
  timestamp?: string;
}

export interface GameSessionEndedEvent {
  session_id: string;
  winner_id?: string | null;
  status: string;
  timestamp?: string;
}

export type GameEvent = 
  | { type: 'game.move.applied'; data: GameMoveAppliedEvent }
  | { type: 'game.session.started'; data: GameSessionStartedEvent }
  | { type: 'game.session.ended'; data: GameSessionEndedEvent };

type GameWebSocketCallbacks = {
  onMoveApplied?: (event: GameMoveAppliedEvent) => void;
  onSessionStarted?: (event: GameSessionStartedEvent) => void;
  onSessionEnded?: (event: GameSessionEndedEvent) => void;
  onError?: (error: Event) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
};

export function useGameWebSocket(sessionId: string | null, callbacks: GameWebSocketCallbacks = {}) {
  const { keycloak } = useKeycloak();
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;
  // Store token in ref to avoid recreating connect callback on token changes
  const tokenRef = useRef<string | undefined>(keycloak.token);
  // Track which sessionId we're currently connected to (if any)
  const connectedSessionIdRef = useRef<string | null>(null);
  
  // Update token ref when it changes
  useEffect(() => {
    tokenRef.current = keycloak.token;
  }, [keycloak.token]);

  const {
    onMoveApplied,
    onSessionStarted,
    onSessionEnded,
    onError,
    onConnect,
    onDisconnect,
  } = callbacks;

  // Store callbacks in refs to avoid recreating connect callback
  const callbacksRef = useRef(callbacks);
  useEffect(() => {
    callbacksRef.current = callbacks;
  }, [callbacks]);

  // Store sessionId in ref to track changes
  const sessionIdRef = useRef<string | null>(sessionId);
  
  // Create stable connect function that doesn't depend on anything
  const connectRef = useRef<(() => void) | null>(null);
  
  // Update sessionId ref
  useEffect(() => {
    sessionIdRef.current = sessionId;
  }, [sessionId]);

  // Define connect function using refs - this will be stable
  const connect = useCallback(() => {
    const currentSessionId = sessionIdRef.current;
    if (!currentSessionId) {
      return;
    }

    // Prevent multiple simultaneous connection attempts to the same session
    if (wsRef.current && 
        connectedSessionIdRef.current === currentSessionId &&
        (wsRef.current.readyState === WebSocket.CONNECTING || wsRef.current.readyState === WebSocket.OPEN)) {
      console.log('[GameWebSocket] Already connected to this session, skipping...');
      return;
    }

    // Close existing connection if any
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    // Get WebSocket URL
    const baseUrl = API_CONFIG.BASE_URL.replace(/^http/, 'ws');
    let wsUrl = `${baseUrl}/api/v1/games/ws/${currentSessionId}`;

    // Add token as query parameter for gateway authentication
    // Note: WebSocket in browsers can't send custom headers, so we use query param
    // Use ref to get latest token without recreating callback
    const token = tokenRef.current;
    if (token) {
      wsUrl += `?token=${encodeURIComponent(token)}`;
    }

    try {
      // Create WebSocket connection
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        console.log(`[GameWebSocket] Connected to session ${currentSessionId}`);
        setConnected(true);
        reconnectAttempts.current = 0;
        connectedSessionIdRef.current = currentSessionId;
        callbacksRef.current.onConnect?.();
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          
          // Handle different event types
          if (message.session_id) {
            // Check for move applied event first (has move_data or move field)
            if (message.move_data !== undefined || message.move !== undefined) {
              // game.move.applied - map move to move_data if needed for consistency
              const moveAppliedEvent = message.move_data !== undefined 
                ? message 
                : { ...message, move_data: message.move };
              callbacksRef.current.onMoveApplied?.(moveAppliedEvent as GameMoveAppliedEvent);
            } 
            // Check for session ended event (status indicates end, but no move_data)
            else if (message.status === 'ended' || message.status === 'completed' || message.status === 'finished') {
              // game.session.ended
              callbacksRef.current.onSessionEnded?.(message as GameSessionEndedEvent);
            }
            // Check for session started event (has game_type but no move_data and not finished)
            else if (message.game_type !== undefined) {
              // game.session.started
              callbacksRef.current.onSessionStarted?.(message as GameSessionStartedEvent);
            }
          }
        } catch (error) {
          console.error('[GameWebSocket] Failed to parse message:', error);
        }
      };

      ws.onerror = (error) => {
        console.error('[GameWebSocket] Error:', error);
        callbacksRef.current.onError?.(error);
      };

      ws.onclose = (event) => {
        console.log(`[GameWebSocket] Disconnected from session ${currentSessionId}`, event.code, event.reason);
        setConnected(false);
        // Only clear connectedSessionId if this was the session we were connected to
        if (connectedSessionIdRef.current === currentSessionId) {
          connectedSessionIdRef.current = null;
        }
        callbacksRef.current.onDisconnect?.();

        // Only reconnect if sessionId hasn't changed and it's not a normal closure
        const shouldReconnect = 
          sessionIdRef.current === currentSessionId && // Session hasn't changed
          event.code !== 1000 && // Not a normal closure
          reconnectAttempts.current < maxReconnectAttempts; // Haven't exceeded max attempts

        if (shouldReconnect) {
          reconnectAttempts.current += 1;
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 10000);
          console.log(`[GameWebSocket] Reconnecting in ${delay}ms (attempt ${reconnectAttempts.current}/${maxReconnectAttempts})`);
          
          reconnectTimeoutRef.current = setTimeout(() => {
            // Check again before reconnecting
            if (sessionIdRef.current === currentSessionId && connectRef.current) {
              connectRef.current();
            }
          }, delay);
        } else {
          reconnectAttempts.current = 0;
        }
      };

      wsRef.current = ws;
    } catch (error) {
      console.error('[GameWebSocket] Failed to create connection:', error);
      callbacksRef.current.onError?.(error as Event);
    }
  }, []); // Empty deps - function is stable

  // Store connect in ref
  connectRef.current = connect;

  // Only run effect when sessionId actually changes
  useEffect(() => {
    // Clean up any existing connection first
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setConnected(false);
    reconnectAttempts.current = 0;
    connectedSessionIdRef.current = null;

    // Connect if we have a sessionId
    if (sessionId) {
      connect();
    }

    return () => {
      // Cleanup on unmount or sessionId change
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      setConnected(false);
      reconnectAttempts.current = 0;
      connectedSessionIdRef.current = null;
    };
  }, [sessionId]);

  const sendMessage = useCallback((message: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    } else {
      console.warn('[GameWebSocket] Cannot send message: WebSocket not connected');
    }
  }, []);

  return {
    connected,
    sendMessage,
    reconnect: connect,
  };
}

