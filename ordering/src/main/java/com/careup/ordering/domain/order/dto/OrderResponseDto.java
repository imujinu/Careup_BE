package com.careup.ordering.domain.order.dto;

import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private Long memberId;
    private String memberName;
    private Long branchId;
    private Long totalAmount;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> orderItems;
}