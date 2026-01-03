"""
Custom Types for Chatbot Module
Type definitions for clear contracts.
"""
from dataclasses import dataclass
from typing import Optional, List


@dataclass
class ChatResponse:
    """Structured chat response."""
    response: str
    conversation_id: str
    sources: Optional[List[str]]
    cached: bool


@dataclass
class ConversationMessage:
    """Single message in a conversation."""
    role: str
    content: str


@dataclass
class ConversationHistory:
    """Complete conversation history."""
    conversation_id: str
    messages: List[ConversationMessage]


