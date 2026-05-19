# FastAPI 챗봇 분리 및 하이브리드 검색 마이그레이션

## 변경 요약

기존 `branch.domain.chat`에 있던 챗봇 오케스트레이션 책임을 새 `chatbot` FastAPI 모듈로 분리했다. `branch`의 기존 `/chatbot/ask` 경로는 유지하며, 요청을 FastAPI `POST /chatbot/ask`로 우선 전달한다. FastAPI 호출이 실패하면 기존 Spring AI 기반 `ChatService#handleUserQuery`로 fallback한다.

배치 작업과 섞이지 않도록 기존 변경은 `wip-batch-bottleneck-before-chatbot-migration` stash로 보존했고, 작업 브랜치는 `feature/chatbot-fastapi-langgraph`로 분리했다.

## 기존 구조와 변경 후 구조

| 구분 | 기존 | 변경 후 |
| --- | --- | --- |
| 진입점 | `branch` `/chatbot/ask` | 동일 경로 유지, FastAPI 우선 프록시 |
| AI orchestration | Spring AI `ChatService` | FastAPI `chatbot/app/graph.py` |
| fallback | 없음 | FastAPI 실패 시 기존 Spring AI 로직 |
| RAG | `branch` Qdrant/Spring AI RAG | FastAPI 문서 업로드/검색 API와 hybrid retriever |
| 정확도 측정 | 없음 | `POST /eval/run`, pytest 기반 핵심 지표 |

## `/chatbot/ask` 전체 요청 흐름

1. 클라이언트가 기존과 동일하게 `branch-service/chatbot/ask`를 호출한다.
2. `ChatController`가 `ChatbotProxyService`를 통해 FastAPI `/chatbot/ask`를 호출한다.
3. FastAPI가 intent/action/parameter를 정규화하고 필요 시 도메인 API 또는 문서 검색을 수행한다.
4. FastAPI 응답이 성공하면 `branch`가 기존 `CommonSuccessDto` 형식으로 감싸 반환한다.
5. FastAPI 호출 실패 시 `branch`의 기존 `ChatService#handleUserQuery`가 실행된다.

## LangGraph 노드 플로우

`chatbot/app/graph.py`는 다음 노드 순서로 동작한다.

| Node | 책임 |
| --- | --- |
| `classify_intent` | SALES, ATTENDANCE, STOCK, ORDER, DOCUMENT 분류 |
| `normalize_params` | 날짜, 발주번호/계약번호/사업자번호/주문번호 추출 |
| `route_action` | intent를 실행 action으로 변환 |
| `call_branch_or_ordering_api` | 매출/재고 등 도메인 API 호출 |
| `retrieve_documents` | 문서형 질문 또는 고유명사 검색 시 RAG 후보 조회 |
| `rerank_contexts` | 설정에 따라 후보 재정렬 |
| `generate_answer` | API 결과와 문서 근거 기반 답변 생성 |
| `format_response` | FastAPI 응답 스키마로 정리 |

LangGraph 패키지가 설치되지 않은 환경에서도 동일 순서의 `FallbackGraph`가 실행되므로 로컬 단위 테스트가 안정적으로 동작한다.

## RAG 업로드/검색/답변 플로우

문서 업로드는 `POST /documents/{branchId}/upload`로 들어오며, 텍스트를 chunk로 나눈 뒤 각 chunk에 아래 metadata를 부여한다.

| Metadata | 설명 |
| --- | --- |
| `branchId` | 지점 범위 필터 |
| `documentId` | 문서 식별자 |
| `sourceFilename` | 원본 파일명 |
| `chunkIndex` | chunk 순번 |
| `text` | 검색/답변 근거 텍스트 |
| `identifiers` | 발주번호, 계약번호, 사업자번호, 주문번호 |

검색은 `POST /documents/query` 또는 `/chatbot/ask`의 DOCUMENT/ORDER 경로에서 수행한다. 결과는 답변의 `contexts`에 포함된다.

## Hybrid Search와 Reranking

발주번호처럼 정확한 고유명사는 dense vector만으로 누락될 수 있어 hybrid search를 기본 정책으로 둔다.

| 단계 | 동작 |
| --- | --- |
| dense | 질문과 chunk 텍스트의 의미/토큰 유사도 기반 후보 생성 |
| sparse | 발주번호, 상품명, 계약번호 등 정확 토큰 매칭 |
| payload filter | `branchId`, `documentId`, `identifiers` 기준 범위 제한 |
| fusion | dense/sparse rank를 RRF 방식으로 결합 |
| identifier boost | 고유명사 감지 시 exact match chunk 가중치 상승 |
| reranking | `RERANK_ENABLED=true`일 때 top 후보를 재정렬 |

현재 테스트 가능한 기본 구현은 deterministic in-memory hybrid retriever다. Docker 환경에서는 Qdrant를 함께 띄우도록 `docker-compose.yml`에 `chatbot` 서비스를 추가했고, Qdrant 컬렉션 이름은 `QDRANT_COLLECTION=careup_chatbot_documents`로 둔다.

## 변경 파일

| 경로 | 변경 |
| --- | --- |
| `chatbot/` | FastAPI 챗봇 모듈 신규 추가 |
| `branch/.../ChatController.java` | FastAPI 우선 프록시, 기존 Spring AI fallback |
| `branch/.../ChatbotProxyService.java` | FastAPI 호출 클라이언트 |
| `branch/src/main/resources/application-*.yml` | `CHATBOT_SERVICE_URL` 설정 |
| `apigateway/src/main/resources/application.yml` | `/chatbot-service/**` 라우트 |
| `docker-compose.yml` | `chatbot` 서비스 추가 |

## 테스트 결과

| 구분 | 명령어 | 결과 |
| --- | --- | --- |
| Python syntax | `python -m compileall chatbot/app` | PASS |
| Python unit | `python -m pytest chatbot/tests` | PASS, 6 passed |
| Eval smoke | `python -c "from app.evals import run_eval; print(run_eval().model_dump())"` in `chatbot` | `intentAccuracy=1.0`, `actionAccuracy=1.0`, `parameterMatchRate=1.0`, `identifierRecallAt5=1.0` |
| Branch compile | `.\gradlew.bat compileJava` in `branch` | PASS |
| Branch test | `.\gradlew.bat test` in `branch` with local dummy env values | PASS |

## 운영 메모

- `branch`의 기존 Spring AI 챗봇은 즉시 삭제하지 않고 deprecated fallback으로 유지한다.
- `CHATBOT_SERVICE_URL` 기본값은 local `http://localhost:8000`, prod `http://chatbot-service:8000`이다.
- `/chatbot-service/**`는 API Gateway에서 FastAPI로 직접 접근할 때 사용한다.
- 고유명사 검색 정확도는 `identifierRecallAt5`, 일반 intent는 `intentAccuracy`, action은 `actionAccuracy`로 측정한다.
