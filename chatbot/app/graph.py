from __future__ import annotations

import re
import uuid
from typing import Any, TypedDict

from app.clients import DomainApiClient
from app.config import settings
from app.retrieval.hybrid import retriever
from app.retrieval.identifiers import extract_identifiers
from app.retrieval.rerank import rerank_contexts as apply_rerank
from app.schemas import ChatbotAskRequest, ChatbotAskResponse, RetrievedContext


class ChatState(TypedDict, total=False):
    request: ChatbotAskRequest
    trace_id: str
    intent: str
    action: str
    parameters: dict[str, Any]
    api_result: dict[str, Any]
    contexts: list[RetrievedContext]
    answer: str
    fallback_used: bool


INTENT_KEYWORDS = {
    "SALES": ["매출", "sales", "정산", "통계"],
    "ATTENDANCE": ["근태", "출근", "퇴근", "근무", "attendance"],
    "STOCK": ["재고", "stock", "입고", "품절"],
    "ORDER": ["발주", "주문", "purchase", "order"],
    "DOCUMENT": ["문서", "계약", "발주번호", "파일", "document", "pdf", "po-"],
}


def classify_intent(state: ChatState) -> ChatState:
    message = state["request"].message.lower()
    for intent, keywords in INTENT_KEYWORDS.items():
        if any(keyword in message for keyword in keywords):
            state["intent"] = intent
            break
    else:
        state["intent"] = "DOCUMENT" if extract_identifiers(message) else "UNKNOWN"
    return state


def normalize_params(state: ChatState) -> ChatState:
    message = state["request"].message
    parameters: dict[str, Any] = {"identifiers": extract_identifiers(message)}
    date_match = re.search(r"\b(20\d{2}-\d{2}-\d{2})\b", message)
    if date_match:
        parameters["date"] = date_match.group(1)
    state["parameters"] = parameters
    return state


def route_action(state: ChatState) -> ChatState:
    intent = state.get("intent", "UNKNOWN")
    message = state["request"].message
    if intent == "DOCUMENT":
        state["action"] = "DOCUMENT_SEARCH"
    elif intent == "ORDER" and extract_identifiers(message):
        state["action"] = "ORDER_LOOKUP"
    elif intent == "SALES":
        state["action"] = "SALES_SUMMARY"
    elif intent == "STOCK":
        state["action"] = "STOCK_LOOKUP"
    elif intent == "ATTENDANCE":
        state["action"] = "ATTENDANCE_LOOKUP"
    else:
        state["action"] = "GENERAL_ANSWER"
    return state


async def call_branch_or_ordering_api(state: ChatState) -> ChatState:
    if state.get("intent") in {"DOCUMENT", "UNKNOWN"}:
        state["api_result"] = {}
        return state
    client = DomainApiClient(settings.branch_service_url, settings.ordering_service_url)
    try:
        state["api_result"] = await client.call_action(
            state.get("intent", "UNKNOWN"),
            state.get("action", "GENERAL_ANSWER"),
            state["request"].branchId,
            state.get("parameters", {}),
        )
    except Exception as exc:  # keep branch fallback alive while FastAPI matures
        state["api_result"] = {"error": str(exc), "fallbackRecommended": True}
        state["fallback_used"] = True
    return state


def retrieve_documents(state: ChatState) -> ChatState:
    if state.get("intent") not in {"DOCUMENT", "ORDER", "UNKNOWN"}:
        state["contexts"] = []
        return state
    state["contexts"] = retriever.search(
        branch_id=state["request"].branchId,
        query=state["request"].message,
        top_k=settings.hybrid_top_k,
    )
    return state


def rerank_contexts(state: ChatState) -> ChatState:
    state["contexts"] = apply_rerank(
        state["request"].message,
        state.get("contexts", []),
        enabled=settings.rerank_enabled,
        top_k=settings.final_top_k,
    )
    return state


def generate_answer(state: ChatState) -> ChatState:
    contexts = state.get("contexts", [])
    api_result = state.get("api_result", {})
    if contexts:
        citations = ", ".join(f"{ctx.sourceFilename}#{ctx.chunkIndex}" for ctx in contexts[:3])
        state["answer"] = f"관련 문서에서 확인한 내용입니다. 근거: {citations}"
    elif api_result.get("fallbackRecommended"):
        state["answer"] = "이 요청은 기존 branch-service 챗봇 fallback에서 처리하는 것이 안전합니다."
    elif api_result:
        state["answer"] = "도메인 API 조회 결과를 확인했습니다."
    else:
        state["answer"] = "질문과 연결되는 도메인이나 문서를 찾지 못했습니다."
    return state


def format_response(state: ChatState) -> ChatbotAskResponse:
    return ChatbotAskResponse(
        answer=state.get("answer", ""),
        intent=state.get("intent", "UNKNOWN"),
        action=state.get("action", "GENERAL_ANSWER"),
        parameters=state.get("parameters", {}),
        contexts=state.get("contexts", [])[:settings.final_top_k],
        fallbackUsed=state.get("fallback_used", False),
        traceId=state.get("trace_id"),
    )


class FallbackGraph:
    async def ainvoke(self, state: ChatState) -> ChatState:
        for node in (classify_intent, normalize_params, route_action):
            state = node(state)
        state = await call_branch_or_ordering_api(state)
        for node in (retrieve_documents, rerank_contexts, generate_answer):
            state = node(state)
        return state


def build_graph():
    try:
        from langgraph.graph import END, StateGraph
    except Exception:
        return FallbackGraph()

    graph = StateGraph(ChatState)
    graph.add_node("classify_intent", classify_intent)
    graph.add_node("normalize_params", normalize_params)
    graph.add_node("route_action", route_action)
    graph.add_node("call_branch_or_ordering_api", call_branch_or_ordering_api)
    graph.add_node("retrieve_documents", retrieve_documents)
    graph.add_node("rerank_contexts", rerank_contexts)
    graph.add_node("generate_answer", generate_answer)

    graph.set_entry_point("classify_intent")
    graph.add_edge("classify_intent", "normalize_params")
    graph.add_edge("normalize_params", "route_action")
    graph.add_edge("route_action", "call_branch_or_ordering_api")
    graph.add_edge("call_branch_or_ordering_api", "retrieve_documents")
    graph.add_edge("retrieve_documents", "rerank_contexts")
    graph.add_edge("rerank_contexts", "generate_answer")
    graph.add_edge("generate_answer", END)
    return graph.compile()


chatbot_graph = build_graph()


async def run_chatbot(request: ChatbotAskRequest) -> ChatbotAskResponse:
    state: ChatState = {"request": request, "trace_id": str(uuid.uuid4())}
    result = await chatbot_graph.ainvoke(state)
    return format_response(result)

