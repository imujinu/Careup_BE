package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductRequestDto {
    private Long productId;
    private Long branchId;
    private String serialNumber;
    private Long stockQuantity;
    private Long safetyStock;
    private Long price;
}

