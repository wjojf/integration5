# Chatbot Module

## Overview

The Chatbot module provides a RAG-based chatbot for game rules and platform guidance.

## Features

- Answer questions about game rules
- Provide platform guidance
- Manage conversation history
- Cache responses for performance

## API Endpoints

- `POST /api/v1/chatbot/chat` - Send chat message
- `GET /api/v1/chatbot/conversation/{conversation_id}` - Get conversation history
- `GET /api/v1/chatbot/health` - Check chatbot status

## Module Structure

```
chatbot/
├── __init__.py      # Module setup
├── models.py        # Domain models
├── dto.py           # API request/response models
├── service.py       # Chatbot service (RAG implementation)
└── api.py           # FastAPI route handlers
```

## Configuration

```env
MODULE_CHATBOT_ENABLED=true
CHATBOT_ENABLED=true
CHATBOT_LLM_PROVIDER=openai
CHATBOT_LLM_MODEL=gpt-4
CHATBOT_LLM_API_KEY=your-api-key
CHATBOT_VECTOR_DB_TYPE=chroma
CHATBOT_VECTOR_DB_PATH=./data/vector_db
CHATBOT_CACHE_ENABLED=true
CHATBOT_CACHE_TTL_SECONDS=3600
```

## Usage Example

```python
from app.modules.chatbot.services.chat_service import ChatbotService

service = ChatbotService()
result = service.chat(
    message="How do I play Connect Four?",
    conversation_id="conv-1",
    user_id="user-1"
)
```

## Future Enhancements

- Vector database integration (Chroma/Pinecone)
- LLM API integration (OpenAI/Anthropic)
- Knowledge base from game rules
- Semantic search


