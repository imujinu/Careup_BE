package com.careup.branch.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    private String id;             // Qdrant Point ID
    private String parentId;       // 부모 청크 ID (Parent-Child 매핑 핵심)
    private String content;        // 실제 텍스트 내용
    private List<Float> vector;    // Dense Vector (임베딩)
    private Map<String, Float> sparseVector; // Sparse Vector (키워드 매칭용)
    private Map<String, Object> metadata;    // {"category": "근태"}
}

