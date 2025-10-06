package com.careup.ordering.domain.order.dto;

import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.entity.OrderType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponseDto {
    private Long orderId;
    private Long memberId;
    private Long branchId;
    private Long totalAmount;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
    private List<OrderItemRequestDto> orderItem;
}
