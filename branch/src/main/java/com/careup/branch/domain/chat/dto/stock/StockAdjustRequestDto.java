package com.careup.branch.domain.chat.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustRequestDto {
    private Long branchProductId;
    private Long quantity;
    private String type;
    private String reason;

    public static StockAdjustRequestDto makeDto(Long productId, Long quantity, String type, String reason){
        return StockAdjustRequestDto.builder()
                .branchProductId(productId)
                .quantity(quantity)
                .type(type)
                .reason(reason)
                .build();
    }
}

