package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustRequestDto {
    private Long branchProductId;
    private Long quantity;
    private String type;
    private String reason;
}

