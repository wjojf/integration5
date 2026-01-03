"""
Chatbot API
FastAPI router for chatbot endpoints.
"""

from typing import List

from fastapi import APIRouter, HTTPException, Header

from app.shared.exceptions import ServiceError
from app.modules.chatbot.services.chat_service import ChatbotService
from .dto import (
    ChatRequest,
    ChatResponse,
    ConversationHistoryResponse,
    ConversationHistoryItem,
    ChatbotHealthResponse,
)


def create_router(service: ChatbotService) -> APIRouter:
    """
    Create FastAPI router for chatbot endpoints.

    This is called from the chatbot module's setup function or main app.
    """
    router = APIRouter(tags=["chatbot"])

    @router.post("/chat", response_model=ChatResponse)
    async def chat(
        request: ChatRequest, 
        authorization: str = Header(None, alias="Authorization")
    ):
        """
        Chat endpoint: send a message to the chatbot and get a response.

        - message: user's question / input
        - conversation_id: optional; if provided, keeps context
        - user_id: optional; for future personalization / logging
        """
        try:
            result = service.chat(
                message=request.message,
                conversation_id=request.conversation_id,
                user_id=request.user_id,
                auth_header=authorization,
            )

        except ServiceError as e:
            # Wrap your domain error into an HTTP error
            raise HTTPException(status_code=500, detail=str(e))
        except Exception as e:
            # Provide more helpful error messages
            error_msg = str(e)
            if "timeout" in error_msg.lower() or "timed out" in error_msg.lower():
                raise HTTPException(
                    status_code=504, 
                    detail="Chatbot service timed out. Please try again."
                )
            elif "circuit" in error_msg.lower() or "breaker" in error_msg.lower():
                raise HTTPException(
                    status_code=503,
                    detail="Chatbot service is temporarily unavailable. Please try again later."
                )
            else:
                raise HTTPException(status_code=500, detail=f"Chatbot error: {error_msg}")

        return ChatResponse(
            response=result.response,
            conversation_id=result.conversation_id,
            sources=result.sources or [],
            cached=result.cached,
        )

    @router.get(
        "/conversation/{conversation_id}",
        response_model=ConversationHistoryResponse,
    )
    async def get_conversation_history(conversation_id: str):
        """
        Get the full conversation history for a given conversation_id.
        """
        try:
            history = service.get_conversation_history(conversation_id)
        except ServiceError as e:
            raise HTTPException(status_code=404, detail=str(e))
        except Exception as e:
            raise HTTPException(
                status_code=500,
                detail=f"Failed to retrieve conversation: {e}",
            )

        items: List[ConversationHistoryItem] = [
            ConversationHistoryItem(role=msg.role, content=msg.content)
            for msg in history.messages
        ]

        return ConversationHistoryResponse(
            conversation_id=history.conversation_id,
            history=items,
        )

    @router.get("/health", response_model=ChatbotHealthResponse)
    async def chatbot_health():
        """
        Simple health endpoint to check if the chatbot is enabled and cache status.
        """
        return ChatbotHealthResponse(
            status="enabled" if service.enabled else "disabled",
            cache_enabled=service.cache is not None,
        )

    return router
