from __future__ import annotations

from app.config import settings
from app.graph import classify_intent, normalize_params, route_action
from app.retrieval.hybrid import LocalHybridRetriever
from app.schemas import ChatbotAskRequest, EvalRunResponse


def run_eval() -> EvalRunResponse:
    samples = [
        ("오늘 매출 알려줘", "SALES", "SALES_SUMMARY"),
        ("재고 부족 상품 보여줘", "STOCK", "STOCK_LOOKUP"),
        ("PO-2026-000123 발주 문서 찾아줘", "ORDER", "ORDER_LOOKUP"),
        ("계약 문서에서 위약금 조항 찾아줘", "DOCUMENT", "DOCUMENT_SEARCH"),
    ]
    intent_hits = 0
    action_hits = 0
    param_hits = 0
    for message, expected_intent, expected_action in samples:
        state = {"request": ChatbotAskRequest(branchId=1, message=message)}
        state = classify_intent(state)
        state = normalize_params(state)
        state = route_action(state)
        intent_hits += int(state["intent"] == expected_intent)
        action_hits += int(state["action"] == expected_action)
        param_hits += int(bool(state["parameters"].get("identifiers")) == ("PO-" in message))

    local = LocalHybridRetriever()
    local.add_document(1, "doc-a", "purchase-a.pdf", "발주번호 PO-2026-000123 공급 단가는 12000원입니다.")
    local.add_document(1, "doc-b", "policy.pdf", "근무 정책과 매출 리포트 문서입니다.")
    identifier_hit = any(result.documentId == "doc-a" for result in local.search(1, "PO-2026-000123", top_k=5))

    total = len(samples)
    return EvalRunResponse(
        sampleCount=total,
        intentAccuracy=round(intent_hits / total, 3),
        actionAccuracy=round(action_hits / total, 3),
        parameterMatchRate=round(param_hits / total, 3),
        identifierRecallAt5=1.0 if identifier_hit else 0.0,
        rerankEnabled=settings.rerank_enabled,
    )

