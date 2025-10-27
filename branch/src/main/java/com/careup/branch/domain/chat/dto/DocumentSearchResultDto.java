package com.careup.branch.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSearchResultDto {

    @Schema(description = "문서 ID")
    private String id;

    @Schema(description = "문서 내용")
    private String content;

    @Schema(description = "문서 메타데이터")
    private Map<String, Object> metadata;

    @Schema(description = "유사도 점수")
    private double score;
}
