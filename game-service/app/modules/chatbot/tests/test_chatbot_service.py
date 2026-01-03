from app.modules.chatbot.services.chat_service import ChatbotService
from app.config import settings


def test_chatbot_disabled_returns_message(monkeypatch):
    # Force chatbot disabled
    monkeypatch.setattr(settings, "CHATBOT_ENABLED", False, raising=False)
    service = ChatbotService()

    result = service.chat("hello")

    assert "disabled" in result.response.lower()
    assert result.conversation_id
    assert result.cached is False


def test_chatbot_basic_response_and_history(monkeypatch):
    # Enable chatbot and cache
    monkeypatch.setattr(settings, "CHATBOT_ENABLED", True, raising=False)
    monkeypatch.setattr(settings, "CHATBOT_CACHE_ENABLED", True, raising=False)

    service = ChatbotService()

    # First call – not cached
    result1 = service.chat("What are the rules of Connect Four?")
    conv_id = result1.conversation_id

    assert conv_id
    assert "connect four" in result1.response.lower()
    assert result1.cached is False
    assert result1.sources is not None

    # Check conversation history
    history = service.get_conversation_history(conv_id)
    assert history.conversation_id == conv_id
    assert len(history.messages) == 2       # user + assistant
    assert history.messages[0].role == "user"
    assert history.messages[1].role == "assistant"

    # Second call with same text – should hit cache
    result2 = service.chat("What are the rules of Connect Four?", conversation_id=conv_id)
    assert result2.response == result1.response
    assert result2.cached is True
    assert result2.conversation_id == conv_id
