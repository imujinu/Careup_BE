from __future__ import annotations

from typing import Iterable

from app.config import settings
from app.retrieval.hybrid import split_text, tokenize
from app.retrieval.identifiers import extract_identifiers
from app.schemas import RetrievedContext


class QdrantHybridStore:
    """Qdrant adapter for dense + sparse + payload-filter retrieval.

    The in-memory retriever is used by tests and offline local runs. This adapter keeps
    the production boundary explicit so the service can move document chunks into Qdrant
    without changing API contracts.
    """

    def __init__(self) -> None:
        try:
            from fastembed import TextEmbedding
            from qdrant_client import QdrantClient
        except Exception as exc:
            raise RuntimeError("qdrant-client and fastembed are required for QdrantHybridStore") from exc

        self.client = QdrantClient(url=settings.qdrant_url)
        self.embedder = TextEmbedding(model_name="sentence-transformers/all-MiniLM-L6-v2")
        self.collection = settings.qdrant_collection

    def embed(self, texts: Iterable[str]) -> list[list[float]]:
        return [vector.tolist() if hasattr(vector, "tolist") else list(vector) for vector in self.embedder.embed(list(texts))]

    def build_payloads(self, branch_id: int, document_id: str, source_filename: str, text: str) -> list[dict]:
        payloads = []
        for chunk_index, chunk in enumerate(split_text(text)):
            payloads.append(
                {
                    "branchId": branch_id,
                    "documentId": document_id,
                    "sourceFilename": source_filename,
                    "chunkIndex": chunk_index,
                    "text": chunk,
                    "identifiers": extract_identifiers(chunk),
                    "sparseTokens": tokenize(chunk),
                }
            )
        return payloads

    def payload_to_context(self, payload: dict, score: float) -> RetrievedContext:
        return RetrievedContext(
            branchId=payload["branchId"],
            documentId=payload["documentId"],
            sourceFilename=payload.get("sourceFilename"),
            chunkIndex=payload.get("chunkIndex", 0),
            text=payload.get("text", ""),
            identifiers=payload.get("identifiers", []),
            score=score,
            scoreDetails={"qdrant": score},
        )

