from __future__ import annotations

import hashlib
import os
from pathlib import Path
from typing import List, Dict, Iterable, Tuple

from app.modules.chatbot.vector.chroma import ChatVectorDB

# Optional: DOCX support
try:
    import docx  # python-docx
except Exception:  # pragma: no cover
    docx = None


DOCS_DIR = Path("docs/chatbot_kb")  # put your real docs here (md/txt/docx)


def _read_text_file(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def _read_docx_file(path: Path) -> str:
    if docx is None:
        raise RuntimeError("python-docx is not installed but a .docx file was found.")
    d = docx.Document(str(path))
    return "\n".join(p.text for p in d.paragraphs if p.text.strip())


def _iter_docs(base_dir: Path) -> Iterable[Tuple[str, str]]:
    """
    Yields (source_name, content).
    source_name should be stable (used in metadata + id hashing).
    """
    if not base_dir.exists():
        raise RuntimeError(f"Docs folder not found: {base_dir.resolve()}")

    for path in sorted(base_dir.rglob("*")):
        if path.is_dir():
            continue
        suffix = path.suffix.lower()
        if suffix not in {".md", ".txt", ".docx"}:
            continue

        if suffix == ".docx":
            content = _read_docx_file(path)
        else:
            content = _read_text_file(path)

        source = str(path.relative_to(base_dir)).replace("\\", "/")
        yield source, content


def _chunk_text(text: str, chunk_size: int = 800, overlap: int = 120, max_chunks: int = 5000) -> List[str]:

    """
    Safe chunking:
    - guarantees forward progress
    - prevents infinite loops
    - prevents memory blowups with max_chunks
    """
    text = (text or "").strip()
    if not text:
        return []

    if overlap >= chunk_size:
        overlap = max(0, chunk_size // 4)

    chunks: List[str] = []
    start = 0
    n = len(text)



    while start < n and len(chunks) < max_chunks:
        end = min(n, start + chunk_size)
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)

        # Guarantee forward progress:
        # next_start must be > start, otherwise we break.
        next_start = end - overlap
        if next_start <= start:
            next_start = end  # jump forward (no overlap) rather than looping forever

        start = next_start

    return chunks



def _stable_id(source: str, chunk_index: int, chunk_text: str) -> str:
    """
    Deterministic id => rerunning ingestion won't duplicate chunks.
    """
    h = hashlib.sha256()
    h.update(source.encode("utf-8"))
    h.update(str(chunk_index).encode("utf-8"))
    h.update(chunk_text.encode("utf-8"))
    return h.hexdigest()[:32]


def ingest_real_docs() -> None:
    db = ChatVectorDB()

    all_ids: List[str] = []
    all_docs: List[str] = []
    all_metas: List[Dict] = []

    for source, content in _iter_docs(DOCS_DIR):
        chunks = _chunk_text(content, chunk_size=800, overlap=120)

        for i, chunk in enumerate(chunks):
            doc_id = _stable_id(source, i, chunk)
            all_ids.append(doc_id)
            all_docs.append(chunk)
            all_metas.append({"source": source, "chunk": i})

    if not all_docs:
        print("[ingest] No documents found to ingest.")
        return

    db.upsert_documents(ids=all_ids, documents=all_docs, metadatas=all_metas)
    print(f"[ingest] Upserted {len(all_docs)} chunks from {DOCS_DIR}. collection_count={db.count()}")


if __name__ == "__main__":
    ingest_real_docs()
