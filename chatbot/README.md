# CareUp Chatbot Service

FastAPI 기반 챗봇 오케스트레이션 모듈입니다. 기존 `branch`의 `/chatbot/ask`는 유지하고, `branch`가 이 서비스로 우선 프록시한 뒤 실패하면 기존 Spring AI 챗봇 로직으로 fallback합니다.

## Run

```bash
cd chatbot
cp .env.example .env
uvicorn app.main:app --reload --port 8000
```

Docker Compose에서는 루트에서 실행합니다.

```bash
docker compose up chatbot qdrant redis
```

## APIs

- `GET /health`
- `POST /chatbot/ask`
- `POST /documents/{branchId}/upload`
- `POST /documents/query`
- `POST /eval/run`

## Flow

`/chatbot/ask`는 LangGraph가 있으면 StateGraph로, 없으면 동일 노드 순서를 가진 fallback graph로 실행됩니다.

1. `classify_intent`
2. `normalize_params`
3. `route_action`
4. `call_branch_or_ordering_api`
5. `retrieve_documents`
6. `rerank_contexts`
7. `generate_answer`
8. `format_response`

## Hybrid Search

문서 chunk metadata는 `branchId`, `documentId`, `sourceFilename`, `chunkIndex`, `text`, `identifiers`를 보관합니다. 발주번호, 계약번호, 사업자번호, 주문번호는 정규식으로 추출합니다.

현재 로컬 테스트 경로는 deterministic in-memory hybrid retriever를 사용합니다. dense 유사도, sparse token match, identifier exact match를 RRF 기반으로 결합합니다. `app/retrieval/qdrant_hybrid.py`에는 Qdrant payload schema와 embedding boundary를 분리해 두었고, Docker 환경에서는 Qdrant를 같은 compose network에서 띄웁니다. `RERANK_ENABLED=true`일 때는 reranker 단계에서 후보 점수를 재정렬할 수 있게 분리되어 있습니다.

## Tests

```bash
python -m pytest chatbot/tests
```
