package com.careup.branch.domain.purchaseOrder.dto;

import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderStatusUpdateDto {
    
    private Long purchaseOrderId;
    private OrderStatus orderStatus; // 변경할 상태
    private String reason;
}

