package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFlowResponseDto {
    private Long flowId;
    private Long branchProductId;
    private Long productId;
    private String productName;
    private Long branchId;
    private Long inQuantity;
    private Long outQuantity;
    private String remark;
    private String createdAt;
}

