"""
WebSocket router for game events.
Provides WebSocket endpoint for real-time game updates.
"""
import logging
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from typing import Optional

from .connection_manager import GameWebSocketManager

logger = logging.getLogger(__name__)


def create_websocket_router(connection_manager: GameWebSocketManager) -> APIRouter:
    """Create WebSocket router for game events."""
    router = APIRouter()
    
    @router.websocket("/games/ws/{session_id}")
    async def websocket_endpoint(websocket: WebSocket, session_id: str):
        """
        WebSocket endpoint for game session events.
        
        Connects to a game session and receives real-time updates:
        - GAME_MOVE_APPLIED: When a move is made
        - GAME_SESSION_STARTED: When session starts
        - GAME_SESSION_ENDED: When session ends
        
        Args:
            session_id: The game session ID to connect to
        """
        try:
            await connection_manager.connect(websocket, session_id)
            logger.info(f"WebSocket endpoint connected - session_id={session_id}")
        except Exception as e:
            logger.error(f"Failed to accept WebSocket connection - session_id={session_id}, error={e}", exc_info=True)
            try:
                await websocket.close(code=1011, reason=f"Connection error: {str(e)}")
            except:
                pass
            return
        
        try:
            # Keep connection alive and handle incoming messages
            # FastAPI/Starlette automatically handles ping/pong frames
            while True:
                # Wait for text messages from client
                # This will raise WebSocketDisconnect when connection is closed
                data = await websocket.receive_text()
                logger.debug(f"Received message from WebSocket - session_id={session_id}, data={data}")
                
                # Echo back or handle client messages if needed
                # For now, we just keep the connection alive
                
        except WebSocketDisconnect:
            logger.info(f"WebSocket disconnected normally - session_id={session_id}")
        except RuntimeError as e:
            # Handle case where receive_text() is called after disconnect
            if "disconnect" in str(e).lower():
                logger.info(f"WebSocket already disconnected - session_id={session_id}")
            else:
                logger.error(f"WebSocket runtime error - session_id={session_id}, error={e}", exc_info=True)
        except Exception as e:
            logger.error(f"WebSocket error - session_id={session_id}, error={e}", exc_info=True)
        finally:
            connection_manager.disconnect(websocket, session_id)
    
    @router.websocket("/games/ws")
    async def websocket_endpoint_query(websocket: WebSocket, session_id: Optional[str] = Query(None)):
        """
        WebSocket endpoint with session_id as query parameter.
        Alternative endpoint format: /games/ws?session_id=...
        """
        if not session_id:
            await websocket.close(code=1008, reason="session_id is required")
            return
        
        await connection_manager.connect(websocket, session_id)
        
        try:
            # Keep connection alive and handle incoming messages
            while True:
                # Wait for any message from client (ping/pong or other)
                data = await websocket.receive_text()
                logger.debug(f"Received message from WebSocket - session_id={session_id}, data={data}")
                
        except WebSocketDisconnect:
            logger.info(f"WebSocket disconnected - session_id={session_id}")
        except Exception as e:
            logger.error(f"WebSocket error - session_id={session_id}, error={e}", exc_info=True)
        finally:
            connection_manager.disconnect(websocket, session_id)
    
    return router

