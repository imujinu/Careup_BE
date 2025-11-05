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
    // 사이즈 정보 (속성 값)
    private Long attributeValueId;
    private String attributeValueName;    // 예: "S", "M", "L"
    private String attributeTypeName;     // 예: "사이즈", "색상"
}

