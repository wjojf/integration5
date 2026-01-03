# tests/modules/chatbot/test_chatbot_api.py

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.modules.chatbot.services.chat_service import ChatbotService
from app.modules.chatbot.api.api import create_router
from app.config import settings


def create_test_app() -> TestClient:
    app = FastAPI()
    service = ChatbotService()
    router = create_router(service)
    # Only add the global API prefix; router already has "/chatbot"
    app.include_router(router, prefix="/api/v1/chatbot")
    return TestClient(app)



def test_chat_endpoint(monkeypatch):
    monkeypatch.setattr(settings, "CHATBOT_ENABLED", True, raising=False)
    monkeypatch.setattr(settings, "CHATBOT_CACHE_ENABLED", True, raising=False)

    client = create_test_app()

    resp = client.post(
        "/api/v1/chatbot/chat",
        json={"message": "How to play Connect Four?", "conversation_id": None, "user_id": "test-user"},
    )
    assert resp.status_code == 200

    data = resp.json()
    assert "response" in data
    assert "conversation_id" in data
    assert data["cached"] is False

    conv_id = data["conversation_id"]

    # Call again to check caching + same conversation_id
    resp2 = client.post(
        "/api/v1/chatbot/chat",
        json={"message": "How to play Connect Four?", "conversation_id": conv_id, "user_id": "test-user"},
    )
    data2 = resp2.json()
    assert data2["conversation_id"] == conv_id
    assert data2["cached"] is True


def test_get_conversation_history(monkeypatch):
    monkeypatch.setattr(settings, "CHATBOT_ENABLED", True, raising=False)
    monkeypatch.setattr(settings, "CHATBOT_CACHE_ENABLED", False, raising=False)

    client = create_test_app()

    # Create conversation
    resp = client.post(
        "/api/v1/chatbot/chat",
        json={"message": "Explain the platform", "conversation_id": None, "user_id": "u1"},
    )
    conv_id = resp.json()["conversation_id"]

    # Fetch history
    history_resp = client.get(f"/api/v1/chatbot/conversation/{conv_id}")
    assert history_resp.status_code == 200

    history = history_resp.json()
    assert history["conversation_id"] == conv_id
    assert len(history["history"]) == 2


def test_chatbot_health(monkeypatch):
    monkeypatch.setattr(settings, "CHATBOT_ENABLED", True, raising=False)

    client = create_test_app()
    resp = client.get("/api/v1/chatbot/health")

    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] in ("enabled", "disabled")
    assert "cache_enabled" in data
