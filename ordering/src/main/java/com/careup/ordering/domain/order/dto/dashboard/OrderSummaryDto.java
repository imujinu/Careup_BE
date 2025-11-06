package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 주문 현황 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
    private Long totalOrders;          // 총 주문 수
    private Long completedOrders;      // 처리 완료 건수
    private Long pendingOrders;        // 대기 중인 건수
    private Long canceledOrders;       // 취소된 건수
    private Map<String, Long> orderStatusDistribution;  // 주문 상태 분포 (차트용)
}

