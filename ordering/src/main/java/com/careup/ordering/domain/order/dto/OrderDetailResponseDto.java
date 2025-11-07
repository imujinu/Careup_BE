package com.careup.ordering.domain.order.dto;

import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.entity.OrderType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderDetailResponseDto {
    private Long orderId;
    private Long memberId;
    private String memberName;
    private Long branchId;
    private String branchName;
    private Long totalAmount;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String cancelledReason;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> orderItems;
//    private PaymentResponseDto payment;
}
