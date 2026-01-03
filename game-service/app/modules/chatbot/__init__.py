# app/modules/chatbot/__init__.py

"""
Chatbot Module
RAG-based chatbot for game rules and platform guidance.
"""
from typing import Optional
from fastapi import APIRouter

from app.shared.container import container
from app.config import settings
from app.modules.chatbot.services.chat_service import ChatbotService
from .types import ChatResponse, ConversationHistory, ConversationMessage
from app.modules.chatbot.api.api import create_router
from .rag.llm_client import OpenAiLlmClient, LlmClient
from app.modules.chatbot.vector.chroma import ChatVectorDB



# Module metadata
MODULE_NAME = "chatbot"
MODULE_DESCRIPTION = "RAG-based chatbot for game rules and platform guidance"
MODULE_VERSION = "1.0.0"


def setup_module(api_prefix: str = "/api/v1") -> Optional[APIRouter]:
    """Setup and configure the chatbot module."""
    if not settings.MODULE_CHATBOT_ENABLED:
        return None

    # 1) Construct LLM client if enabled in config
    llm_client: Optional[LlmClient] = None
    if settings.CHATBOT_USE_LLM:
        try:
            llm_client = OpenAiLlmClient()
        except Exception as e:
            print(f"[chatbot] Failed to initialize OpenAI client, using placeholder mode: {e}")
            llm_client = None

    # 2) Construct vector DB (Chroma)
    vector_db = None
    try:
        vector_db = ChatVectorDB()
        print(f"[chatbot] Vector DB ready. count={vector_db.count()} path={settings.CHATBOT_VECTOR_DB_PATH}")
    except Exception as e:
        print(f"[chatbot] Vector DB not available: {e}. Continuing without RAG DB.")
        vector_db = None

    # 3) Register service once (inject both)
    if not container.has(ChatbotService):
        try:
            chatbot_service = ChatbotService(llm_client=llm_client, vector_db=vector_db)
            container.register(
                ChatbotService,
                instance=chatbot_service,
                singleton=True,
            )
            print("[chatbot] ChatbotService initialized successfully")
        except Exception as e:
            print(f"[chatbot] Failed to initialize ChatbotService: {e}")
            import traceback
            traceback.print_exc()
            return None

    try:
        router = create_router(container.get(ChatbotService))
        print("[chatbot] Chatbot router created successfully")
        return router
    except Exception as e:
        print(f"[chatbot] Failed to create router: {e}")
        import traceback
        traceback.print_exc()
        return None

__all__ = [
    "ChatbotService",
    "ChatResponse",
    "ConversationHistory",
    "ConversationMessage",
    "setup_module",
    "MODULE_NAME",
    "MODULE_DESCRIPTION",
    "MODULE_VERSION",
]
