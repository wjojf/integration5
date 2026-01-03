"""
Chatbot DTOs
Data Transfer Objects for chatbot API.
"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict


class ChatRequest(BaseModel):
    """Request for chatbot interaction."""
    message: str = Field(..., description="User's message/question")
    conversation_id: Optional[str] = Field(None, description="Optional conversation ID for context")
    user_id: Optional[str] = Field(None, description="Optional user ID")


class ChatResponse(BaseModel):
    """Response from chatbot."""
    response: str = Field(..., description="Chatbot's response")
    conversation_id: str = Field(..., description="Conversation ID for context")
    sources: Optional[List[str]] = Field(None, description="Sources used to generate response")
    cached: bool = Field(default=False, description="Whether response was from cache")


class ConversationHistoryItem(BaseModel):
    """Single item in conversation history."""
    role: str
    content: str


class ConversationHistoryResponse(BaseModel):
    """Response containing conversation history."""
    conversation_id: str
    history: List[ConversationHistoryItem]


class ChatbotHealthResponse(BaseModel):
    """Response from chatbot health check."""
    status: str
    cache_enabled: bool


