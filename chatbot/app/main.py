from __future__ import annotations

import uuid

from fastapi import FastAPI, File, Form, UploadFile

from app.evals import run_eval
from app.graph import run_chatbot
from app.retrieval.hybrid import retriever
from app.retrieval.identifiers import extract_identifiers
from app.retrieval.rerank import rerank_contexts
from app.schemas import (
    ChatbotAskRequest,
    ChatbotAskResponse,
    DocumentQueryRequest,
    DocumentQueryResponse,
    DocumentUploadResponse,
    EvalRunResponse,
)

app = FastAPI(title="CareUp Chatbot Service", version="0.1.0")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/chatbot/ask", response_model=ChatbotAskResponse)
async def ask(request: ChatbotAskRequest) -> ChatbotAskResponse:
    return await run_chatbot(request)


@app.post("/documents/{branchId}/upload", response_model=DocumentUploadResponse)
async def upload_document(
    branchId: int,
    file: UploadFile = File(...),
    documentId: str | None = Form(default=None),
) -> DocumentUploadResponse:
    raw = await file.read()
    text = raw.decode("utf-8", errors="ignore")
    resolved_document_id = documentId or str(uuid.uuid4())
    chunk_count = retriever.add_document(branchId, resolved_document_id, file.filename or "uploaded-document", text)
    return DocumentUploadResponse(
        branchId=branchId,
        documentId=resolved_document_id,
        sourceFilename=file.filename or "uploaded-document",
        chunkCount=chunk_count,
        identifiers=extract_identifiers(text),
    )


@app.post("/documents/query", response_model=DocumentQueryResponse)
async def query_documents(request: DocumentQueryRequest) -> DocumentQueryResponse:
    candidates = retriever.search(request.branchId, request.query, request.documentId, top_k=max(request.topK, 20))
    results = rerank_contexts(request.query, candidates, enabled=False, top_k=request.topK)
    return DocumentQueryResponse(query=request.query, identifiers=extract_identifiers(request.query), results=results)


@app.post("/eval/run", response_model=EvalRunResponse)
async def eval_run() -> EvalRunResponse:
    return run_eval()

