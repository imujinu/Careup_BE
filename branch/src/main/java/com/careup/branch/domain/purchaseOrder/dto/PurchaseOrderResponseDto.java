package com.careup.branch.domain.purchaseOrder.dto;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDto extends BaseTimeEntity {
    
    private Long purchaseOrderId;
    private Long branchId;
    private OrderStatus orderStatus;
    private long totalPrice; // 총 금액
    
    private List<PurchaseOrderDetailResponseDto> orderDetails; // 발주 상세 내역

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailResponseDto {
        private Long purchaseOrderDetailId; // 발주 상세 ID
        private Long productId; // 상품 ID
        private int quantity; // 요청 수량
        private int approvedQuantity; // 승인 수량
        private long unitPrice; // 단가
        private long subtotalPrice; // 소계
    }
}