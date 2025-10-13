package com.careup.ordering.domain.product.dto;

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
public class PurchaseOrderEventDto {
    
    private Long purchaseOrderId;
    private Long branchId;
    private OrderStatus orderStatus;
    private long totalPrice;
    
    private List<PurchaseOrderDetailEventDto> orderDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PARTIAL,
        SHIPPED,
        COMPLETED
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailEventDto {
        private Long purchaseOrderDetailId;
        private Long productId;
        private int quantity;
        private int approvedQuantity;
        private long unitPrice;
        private long subtotalPrice;
    }
}
