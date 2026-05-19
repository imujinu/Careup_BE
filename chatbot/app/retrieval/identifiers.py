from __future__ import annotations

import re


IDENTIFIER_PATTERNS = [
    re.compile(r"\bPO-\d{4}-\d{4,8}\b", re.IGNORECASE),
    re.compile(r"\bPURCHASE-\d{3,12}\b", re.IGNORECASE),
    re.compile(r"\bORDER-\d{3,12}\b", re.IGNORECASE),
    re.compile(r"\bCONTRACT-\d{3,12}\b", re.IGNORECASE),
    re.compile(r"\b\d{3}-\d{2}-\d{5}\b"),
    re.compile(r"(?<!\d)\d{8,12}(?!\d)"),
]


def normalize_identifier(value: str) -> str:
    return value.strip().upper()


def extract_identifiers(text: str | None) -> list[str]:
    if not text:
        return []
    found: list[str] = []
    for pattern in IDENTIFIER_PATTERNS:
        found.extend(normalize_identifier(match.group(0)) for match in pattern.finditer(text))
    return sorted(set(found))

