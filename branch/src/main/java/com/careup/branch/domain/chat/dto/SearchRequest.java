package com.careup.branch.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequest {
    private String query;
    private String category; // 메타데이터 필터링용
    private int topK = 5;
}
