from __future__ import annotations

from app.retrieval.hybrid import tokenize
from app.schemas import RetrievedContext


def rerank_contexts(query: str, contexts: list[RetrievedContext], enabled: bool, top_k: int) -> list[RetrievedContext]:
    if not enabled:
        return contexts[:top_k]

    query_terms = set(tokenize(query))
    reranked = []
    for context in contexts:
        text_terms = set(tokenize(context.text))
        lexical_overlap = len(query_terms.intersection(text_terms))
        identifier_bonus = 2 if set(context.identifiers).intersection({term.upper() for term in query_terms}) else 0
        adjusted = context.model_copy()
        adjusted.score = round(context.score + lexical_overlap * 0.05 + identifier_bonus, 6)
        adjusted.scoreDetails = {**context.scoreDetails, "rerank": adjusted.score - context.score}
        reranked.append(adjusted)
    return sorted(reranked, key=lambda result: result.score, reverse=True)[:top_k]

