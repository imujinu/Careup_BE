package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 매출 현황 카드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryCardDto {
    private Long totalSales;           // 총 매출
    private Long monthlySales;         // 월간 매출
    private Long totalOrders;          // 총 주문 수
    private List<DailySalesDto> last7DaysSales;  // 최근 7일 매출 (차트)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySalesDto {
        private LocalDate date;
        private Long sales;
    }
}

