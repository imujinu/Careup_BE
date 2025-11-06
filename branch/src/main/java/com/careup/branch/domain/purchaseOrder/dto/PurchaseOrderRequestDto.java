package com.careup.branch.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequestDto {
    
    private Long branchId;
    
    private List<PurchaseOrderDetailRequestDto> orderDetails; // 발주 상세 내역
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailRequestDto {
        private Long productId; // 상품 ID
        private Long branchProductId; // 가맹점 BranchProduct ID
        private Long attributeValueId; // 속성 값 ID (본사 재고 예약용)
        private int quantity; // 수량
        private Long supplyPrice; // 공급가
    }

    public PurchaseOrderRequestDto makeOrder(Long branchId, List<PurchaseOrderDetailRequestDto> list){
        return PurchaseOrderRequestDto.builder()
                .branchId(branchId)
                .orderDetails(list)
                .build();
    }

    public PurchaseOrderDetailRequestDto makeOrderDetail(Long productId, int quantity){
        return PurchaseOrderDetailRequestDto.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }
}

