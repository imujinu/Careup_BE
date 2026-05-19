from app.retrieval.identifiers import extract_identifiers


def test_extract_purchase_and_business_identifiers():
    text = "발주번호 PO-2026-000123, PURCHASE-123, 사업자번호 123-45-67890 확인"
    assert extract_identifiers(text) == ["123-45-67890", "PO-2026-000123", "PURCHASE-123"]


def test_extract_numeric_order_id():
    assert extract_identifiers("주문 202605180001 상태 알려줘") == ["202605180001"]

