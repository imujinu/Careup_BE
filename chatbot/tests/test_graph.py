from app.graph import classify_intent, normalize_params, route_action
from app.schemas import ChatbotAskRequest


def test_order_identifier_routes_to_order_lookup():
    state = {"request": ChatbotAskRequest(branchId=1, message="PO-2026-000123 발주 상태 찾아줘")}
    state = classify_intent(state)
    state = normalize_params(state)
    state = route_action(state)

    assert state["intent"] == "ORDER"
    assert state["action"] == "ORDER_LOOKUP"
    assert state["parameters"]["identifiers"] == ["PO-2026-000123"]


def test_document_keyword_routes_to_document_search():
    state = {"request": ChatbotAskRequest(branchId=1, message="계약 문서에서 해지 조항 찾아줘")}
    state = classify_intent(state)
    state = normalize_params(state)
    state = route_action(state)

    assert state["intent"] == "DOCUMENT"
    assert state["action"] == "DOCUMENT_SEARCH"

