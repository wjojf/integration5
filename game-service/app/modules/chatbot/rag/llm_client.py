from typing import Protocol, Optional

import os
from openai import OpenAI
from app.config import settings


class LlmClient(Protocol):
    """Simple protocol for any chat-based LLM client."""

    def chat(self, system_prompt: str, user_prompt: str) -> str:
        ...


class OpenAiLlmClient:
    """
    OpenAI GPT-based implementation of LlmClient.

    Uses the chat.completions API and a model defined in settings.OPENAI_CHAT_MODEL.
    """

    def __init__(
            self,
            api_key: Optional[str] = None,
            model: Optional[str] = None,
    ) -> None:
        # API key: prefer explicit argument, then settings, then env var
        key = api_key or settings.OPENAI_API_KEY or os.getenv("OPENAI_API_KEY")
        if not key:
            raise RuntimeError("OPENAI_API_KEY is not configured.")

        self.client = OpenAI(api_key=key)
        self.model = model or settings.OPENAI_CHAT_MODEL

    def chat(self, system_prompt: str, user_prompt: str) -> str:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.2,
        )
        msg = response.choices[0].message.content
        return msg or ""
