package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductResponseDto {
    private Long branchProductId;
    private Long productId;
    private Long branchId;
    private String serialNumber;
    private Long stockQuantity;
    private Long safetyStock;
    private Long price;
    private String productName;
    private String productDescription;
}

