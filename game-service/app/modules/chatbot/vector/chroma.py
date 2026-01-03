

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import List, Optional, Dict, Any

import chromadb
from chromadb.config import Settings as ChromaSettings
from chromadb.utils import embedding_functions

from app.config import settings


@dataclass
class VectorHit:
    id: str
    document: str
    metadata: Dict[str, Any]
    distance: float


class ChatVectorDB:
    """
    Chroma-backed vector DB for chatbot knowledge base.

    - Uses PersistentClient => writes to disk at settings.CHATBOT_VECTOR_DB_PATH
    - Uses OpenAI embedding function by default
    """

    def __init__(
        self,
        persist_path: Optional[str] = None,
        collection_name: str = "banditgames_chatbot_kb",
    ) -> None:
        self.persist_path = persist_path or settings.CHATBOT_VECTOR_DB_PATH
        os.makedirs(self.persist_path, exist_ok=True)

        # Persistent client (local disk)
        self.client = chromadb.PersistentClient(
            path=self.persist_path,
            settings=ChromaSettings(anonymized_telemetry=False),
        )

        # Embeddings: OpenAI (fits your existing OpenAI usage in llm_client) :contentReference[oaicite:5]{index=5}
        if not settings.OPENAI_API_KEY:
            raise RuntimeError(
                "OPENAI_API_KEY must be set to use OpenAI embeddings for the chatbot vector DB."
            )

        self.embedding_fn = embedding_functions.OpenAIEmbeddingFunction(
            api_key=settings.OPENAI_API_KEY,
            model_name=settings.CHATBOT_EMBEDDING_MODEL,
        )

        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            embedding_function=self.embedding_fn,
            metadata={"domain": "chatbot_kb"},
        )

    def upsert_documents(
        self,
        ids: List[str],
        documents: List[str],
        metadatas: Optional[List[Dict[str, Any]]] = None,
    ) -> None:
        if len(ids) != len(documents):
            raise ValueError("ids and documents must have the same length")
        if metadatas is not None and len(metadatas) != len(documents):
            raise ValueError("metadatas must match documents length")

        self.collection.upsert(
            ids=ids,
            documents=documents,
            metadatas=metadatas,
        )

    def query(self, query_text: str, top_k: int) -> List[VectorHit]:
        res = self.collection.query(
            query_texts=[query_text],
            n_results=top_k,
            include=["documents", "metadatas", "distances"],
        )

        ids = res.get("ids", [[]])[0] or []
        docs = res.get("documents", [[]])[0] or []
        metas = res.get("metadatas", [[]])[0] or []
        dists = res.get("distances", [[]])[0] or []

        hits: List[VectorHit] = []
        for i in range(len(ids)):
            hits.append(
                VectorHit(
                    id=str(ids[i]),
                    document=str(docs[i]) if docs[i] is not None else "",
                    metadata=metas[i] or {},
                    distance=float(dists[i]) if dists[i] is not None else 0.0,
                )
            )
        return hits

    def count(self) -> int:
        # Chroma supports count on collections
        return self.collection.count()
