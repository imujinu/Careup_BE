from __future__ import annotations

import math
import re
from collections import Counter
from dataclasses import dataclass, field

from app.retrieval.identifiers import extract_identifiers
from app.schemas import RetrievedContext


TOKEN_RE = re.compile(r"[A-Za-z0-9가-힣_-]+")


def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_RE.findall(text or "")]


def reciprocal_rank_fusion(rank: int, k: int = 60) -> float:
    return 1.0 / (k + rank)


@dataclass
class IndexedChunk:
    branch_id: int
    document_id: str
    source_filename: str
    chunk_index: int
    text: str
    identifiers: list[str] = field(default_factory=list)

    def to_context(self, score: float, details: dict[str, float]) -> RetrievedContext:
        return RetrievedContext(
            branchId=self.branch_id,
            documentId=self.document_id,
            sourceFilename=self.source_filename,
            chunkIndex=self.chunk_index,
            text=self.text,
            identifiers=self.identifiers,
            score=round(score, 6),
            scoreDetails={key: round(value, 6) for key, value in details.items()},
        )


class LocalHybridRetriever:
    """Deterministic in-memory hybrid retriever used for tests and local fallback."""

    def __init__(self) -> None:
        self._chunks: list[IndexedChunk] = []

    def add_document(self, branch_id: int, document_id: str, source_filename: str, text: str) -> int:
        chunks = split_text(text)
        for index, chunk in enumerate(chunks):
            self._chunks.append(
                IndexedChunk(
                    branch_id=branch_id,
                    document_id=document_id,
                    source_filename=source_filename,
                    chunk_index=index,
                    text=chunk,
                    identifiers=extract_identifiers(chunk),
                )
            )
        return len(chunks)

    def search(self, branch_id: int, query: str, document_id: str | None = None, top_k: int = 5) -> list[RetrievedContext]:
        query_tokens = tokenize(query)
        query_identifier_set = set(extract_identifiers(query))
        candidates = [
            chunk for chunk in self._chunks
            if chunk.branch_id == branch_id and (document_id is None or chunk.document_id == document_id)
        ]
        if not candidates:
            return []

        dense_ranked = sorted(candidates, key=lambda chunk: dense_score(query_tokens, tokenize(chunk.text)), reverse=True)
        sparse_ranked = sorted(candidates, key=lambda chunk: sparse_score(query_tokens, tokenize(chunk.text)), reverse=True)

        scores: dict[tuple[str, int], dict[str, float]] = {}
        for rank, chunk in enumerate(dense_ranked, start=1):
            key = (chunk.document_id, chunk.chunk_index)
            scores.setdefault(key, {})["dense"] = reciprocal_rank_fusion(rank)
        for rank, chunk in enumerate(sparse_ranked, start=1):
            key = (chunk.document_id, chunk.chunk_index)
            scores.setdefault(key, {})["sparse"] = reciprocal_rank_fusion(rank)

        by_key = {(chunk.document_id, chunk.chunk_index): chunk for chunk in candidates}
        ranked: list[RetrievedContext] = []
        for key, details in scores.items():
            chunk = by_key[key]
            exact = 1.0 if query_identifier_set.intersection(chunk.identifiers) else 0.0
            details["identifier"] = exact
            identifier_weight = 3.0 if query_identifier_set else 1.0
            total = details.get("dense", 0.0) + details.get("sparse", 0.0) + exact * identifier_weight
            ranked.append(chunk.to_context(total, details))

        return sorted(ranked, key=lambda result: result.score, reverse=True)[:top_k]


def dense_score(query_tokens: list[str], doc_tokens: list[str]) -> float:
    if not query_tokens or not doc_tokens:
        return 0.0
    query_counts = Counter(query_tokens)
    doc_counts = Counter(doc_tokens)
    dot = sum(query_counts[token] * doc_counts[token] for token in query_counts)
    query_norm = math.sqrt(sum(value * value for value in query_counts.values()))
    doc_norm = math.sqrt(sum(value * value for value in doc_counts.values()))
    return dot / (query_norm * doc_norm) if query_norm and doc_norm else 0.0


def sparse_score(query_tokens: list[str], doc_tokens: list[str]) -> float:
    if not query_tokens or not doc_tokens:
        return 0.0
    doc_set = set(doc_tokens)
    return sum(2.0 if token in doc_set and ("-" in token or token.isdigit()) else 1.0 for token in query_tokens if token in doc_set)


def split_text(text: str, max_chars: int = 900, overlap: int = 120) -> list[str]:
    normalized = " ".join((text or "").split())
    if not normalized:
        return []
    chunks: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(len(normalized), start + max_chars)
        chunks.append(normalized[start:end])
        if end == len(normalized):
            break
        start = max(0, end - overlap)
    return chunks


retriever = LocalHybridRetriever()

