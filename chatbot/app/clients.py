from __future__ import annotations

from typing import Any

import httpx


class DomainApiClient:
    def __init__(self, branch_base_url: str, ordering_base_url: str, timeout: float = 5.0) -> None:
        self.branch_base_url = branch_base_url.rstrip("/")
        self.ordering_base_url = ordering_base_url.rstrip("/")
        self.timeout = timeout

    async def call_action(self, intent: str, action: str, branch_id: int, parameters: dict[str, Any]) -> dict[str, Any]:
        if intent == "SALES":
            path = f"{self.ordering_base_url}/sales/statistics"
            return await self._get(path, {"branchId": branch_id, **parameters})
        if intent == "STOCK":
            path = f"{self.ordering_base_url}/inventory/branches/{branch_id}/stock"
            return await self._get(path, parameters)
        if intent == "ORDER":
            return {"message": "발주 생성/수정은 branch-service의 기존 fallback 로직에서 처리됩니다.", "fallbackRecommended": True}
        if intent == "ATTENDANCE":
            return {"message": "근태 도메인 액션은 branch-service fallback 로직에서 처리됩니다.", "fallbackRecommended": True}
        return {}

    async def _get(self, url: str, params: dict[str, Any]) -> dict[str, Any]:
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url, params=params)
            response.raise_for_status()
            payload = response.json()
            return payload if isinstance(payload, dict) else {"result": payload}

