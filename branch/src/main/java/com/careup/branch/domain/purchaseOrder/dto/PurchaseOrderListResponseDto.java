package com.careup.branch.domain.purchaseOrder.dto;

import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderListResponseDto {
    private Long purchaseOrderId;
    private Long branchId;
    private String branchName;
    private OrderStatus orderStatus;
    private Long totalPrice;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}