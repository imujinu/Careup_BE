from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class ChatbotAskRequest(BaseModel):
    branchId: int = Field(..., description="Branch id from the existing branch chatbot API")
    message: str
    employeeId: int | None = None
    locale: str = "ko-KR"


class RetrievedContext(BaseModel):
    branchId: int
    documentId: str
    sourceFilename: str | None = None
    chunkIndex: int = 0
    text: str
    identifiers: list[str] = Field(default_factory=list)
    score: float = 0.0
    scoreDetails: dict[str, float] = Field(default_factory=dict)


class ChatbotAskResponse(BaseModel):
    answer: str
    intent: str
    action: str
    parameters: dict[str, Any] = Field(default_factory=dict)
    contexts: list[RetrievedContext] = Field(default_factory=list)
    fallbackUsed: bool = False
    traceId: str | None = None


class DocumentUploadResponse(BaseModel):
    branchId: int
    documentId: str
    sourceFilename: str
    chunkCount: int
    identifiers: list[str]


class DocumentQueryRequest(BaseModel):
    branchId: int
    query: str
    documentId: str | None = None
    topK: int = 5


class DocumentQueryResponse(BaseModel):
    query: str
    identifiers: list[str]
    results: list[RetrievedContext]


class EvalRunResponse(BaseModel):
    sampleCount: int
    intentAccuracy: float
    actionAccuracy: float
    parameterMatchRate: float
    identifierRecallAt5: float
    rerankEnabled: bool

