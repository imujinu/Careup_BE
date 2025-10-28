package com.careup.branch.domain.chat.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
        private String query;
        private String model; // Ollama 모델명
        private int maxResults; // vector search 갯수
}
