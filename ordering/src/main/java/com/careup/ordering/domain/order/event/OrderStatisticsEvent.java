package com.careup.ordering.domain.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 주문 통계 이벤트
 * 주문이 생성되거나 상태가 변경될 때 발행되는 이벤트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long branchId;
    private Long totalAmount;
    private String orderStatus; // PENDING, CONFIRMED, CANCELLED
    private String previousStatus; // 상태 변경 시 이전 상태
    private LocalDateTime orderDateTime;
    private String eventType; // CREATE, UPDATE, DELETE
}

