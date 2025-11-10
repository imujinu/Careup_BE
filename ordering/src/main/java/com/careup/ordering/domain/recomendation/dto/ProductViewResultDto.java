package com.careup.ordering.domain.recomendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductViewResultDto {
    private Long productId;
    private boolean isExist; // 다른 API 호출 필요 여부
}
