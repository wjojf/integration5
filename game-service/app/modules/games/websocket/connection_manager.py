"""
WebSocket connection manager for game sessions.
Manages active WebSocket connections grouped by session_id.
"""
import logging
from typing import Dict, Set
from fastapi import WebSocket
import json

logger = logging.getLogger(__name__)


class GameWebSocketManager:
    """Manages WebSocket connections for game sessions."""
    
    def __init__(self):
        # Map session_id -> Set of WebSocket connections
        self._connections: Dict[str, Set[WebSocket]] = {}
    
    async def connect(self, websocket: WebSocket, session_id: str) -> None:
        """Accept and register a WebSocket connection for a session."""
        await websocket.accept()
        
        if session_id not in self._connections:
            self._connections[session_id] = set()
        
        self._connections[session_id].add(websocket)
        logger.info(f"WebSocket connected - session_id={session_id}, total_connections={len(self._connections[session_id])}")
    
    def disconnect(self, websocket: WebSocket, session_id: str) -> None:
        """Remove a WebSocket connection."""
        if session_id in self._connections:
            self._connections[session_id].discard(websocket)
            
            # Clean up empty sets
            if not self._connections[session_id]:
                del self._connections[session_id]
                logger.info(f"All connections closed for session_id={session_id}")
            else:
                logger.info(f"WebSocket disconnected - session_id={session_id}, remaining_connections={len(self._connections[session_id])}")
    
    async def broadcast_to_session(self, session_id: str, message: dict) -> None:
        """Broadcast a message to all connections for a session."""
        if session_id not in self._connections:
            logger.debug(f"No connections for session_id={session_id}")
            return
        
        disconnected = set()
        message_json = json.dumps(message)
        
        for websocket in self._connections[session_id]:
            try:
                await websocket.send_text(message_json)
            except Exception as e:
                logger.warning(f"Failed to send message to WebSocket - session_id={session_id}, error={e}")
                disconnected.add(websocket)
        
        # Remove disconnected websockets
        for ws in disconnected:
            self.disconnect(ws, session_id)
    
    def get_connection_count(self, session_id: str) -> int:
        """Get the number of active connections for a session."""
        return len(self._connections.get(session_id, set()))
    
    def get_all_sessions(self) -> Set[str]:
        """Get all session IDs with active connections."""
        return set(self._connections.keys())

