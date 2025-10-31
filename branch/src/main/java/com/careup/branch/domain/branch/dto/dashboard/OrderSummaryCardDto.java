package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 주문 현황 카드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryCardDto {
    private Long totalOrders;          // 총 주문 수
    private Long completedOrders;      // 처리 완료 건수
    private Long pendingOrders;        // 대기 중인 건수
    private Long canceledOrders;       // 취소된 건수
    private Map<String, Long> orderStatusDistribution;  // 주문 상태 분포 (차트용)
}

