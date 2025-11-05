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
    private Long attributeValueId; // 사이즈별 재고 관리를 위한 속성 값 ID (선택사항)
}