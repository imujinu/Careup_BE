package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 매출 현황 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDto {
    private Long totalSales;           // 총 매출
    private Long monthlySales;         // 월간 매출
    private Long totalOrders;          // 총 주문 수
    private List<DailySalesDto> last7DaysSales;  // 최근 7일 매출 (차트용)
}
