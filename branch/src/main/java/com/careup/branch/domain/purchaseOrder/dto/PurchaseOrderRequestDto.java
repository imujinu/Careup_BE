package com.careup.branch.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequestDto {
    
    private Long branchId;
    
    private List<PurchaseOrderDetailRequestDto> orderDetails; // 발주 상세 내역
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailRequestDto {
        private Long productId; // 상품 ID
        private int quantity; // 수량
        private Long unitPrice; // 단가
    }
}

