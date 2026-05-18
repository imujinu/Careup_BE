from app.retrieval.hybrid import LocalHybridRetriever


def test_identifier_query_ranks_exact_chunk_first():
    retriever = LocalHybridRetriever()
    retriever.add_document(1, "doc-a", "a.txt", "발주번호 PO-2026-000123 공급 단가는 12000원입니다.")
    retriever.add_document(1, "doc-b", "b.txt", "일반 재고 관리 정책 문서입니다.")

    results = retriever.search(1, "PO-2026-000123 발주 찾아줘", top_k=5)

    assert results[0].documentId == "doc-a"
    assert results[0].scoreDetails["identifier"] == 1.0


def test_branch_filter_excludes_other_branch_documents():
    retriever = LocalHybridRetriever()
    retriever.add_document(1, "doc-a", "a.txt", "PO-2026-000123")
    retriever.add_document(2, "doc-b", "b.txt", "PO-2026-000123")

    results = retriever.search(2, "PO-2026-000123", top_k=5)

    assert [result.documentId for result in results] == ["doc-b"]

