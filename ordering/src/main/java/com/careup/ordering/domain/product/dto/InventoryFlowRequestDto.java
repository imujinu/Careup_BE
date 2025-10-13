package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFlowRequestDto {
    private Long branchProductId;
    private Long inQuantity;
    private Long outQuantity;
    private String remark;
}

