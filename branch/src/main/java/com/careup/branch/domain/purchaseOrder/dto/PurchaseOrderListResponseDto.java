package com.careup.branch.domain.purchaseOrder.dto;

import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderListResponseDto {

    private Long purchaseOrderId;
    private Long branchId;
    private OrderStatus orderStatus; // 발주 상태
    private Long totalPrice; // 총 금액
    private Integer productCount; // 상품 수
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
