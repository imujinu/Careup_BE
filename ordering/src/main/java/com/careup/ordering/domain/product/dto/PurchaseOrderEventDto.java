package com.careup.ordering.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
        COMPLETED,
        CANCELLED
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PurchaseOrderDetailEventDto {
        private Long purchaseOrderDetailId;
        private Long productId;
        private String productName; // branch에서 전송되는 필드, 무시됨
        private String categoryName; // branch에서 전송되는 필드, 무시됨
        private int quantity;
        private int approvedQuantity;
        private long unitPrice;
        private long subtotalPrice;
    }
}
