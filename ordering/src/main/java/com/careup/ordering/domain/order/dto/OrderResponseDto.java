package com.careup.ordering.domain.order.dto;

import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.entity.OrderType;
import com.careup.ordering.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
    private String branchName;
    private Long totalAmount;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approvedByName;
    private String rejectedReason;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String cancelledReason;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> orderItems;
    // 결제 정보 추가
    private PaymentStatus paymentStatus;  // 결제 상태
    private Boolean isPaymentCompleted; // 결제 완료 여부
}