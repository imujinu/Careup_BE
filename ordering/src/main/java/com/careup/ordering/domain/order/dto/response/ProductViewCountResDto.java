package com.careup.ordering.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductViewCountResDto {
    private Long productId;
    private boolean hasRecentView;

    public static ProductViewCountResDto makeDto(Long productId, Boolean hasRecentView){
        return ProductViewCountResDto.builder()
                .productId(productId)
                .hasRecentView(hasRecentView)
                .build();
    }
}
