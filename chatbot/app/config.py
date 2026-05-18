import os
from dataclasses import dataclass


def _bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    app_env: str = os.getenv("APP_ENV", "local")
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    branch_service_url: str = os.getenv("BRANCH_SERVICE_URL", "http://localhost:8080/branch-service")
    ordering_service_url: str = os.getenv("ORDERING_SERVICE_URL", "http://localhost:8080/ordering-service")
    qdrant_url: str = os.getenv("QDRANT_URL", "http://localhost:6333")
    qdrant_collection: str = os.getenv("QDRANT_COLLECTION", "careup_chatbot_documents")
    rerank_enabled: bool = _bool("RERANK_ENABLED", False)
    hybrid_top_k: int = int(os.getenv("HYBRID_TOP_K", "20"))
    final_top_k: int = int(os.getenv("FINAL_TOP_K", "5"))


settings = Settings()

