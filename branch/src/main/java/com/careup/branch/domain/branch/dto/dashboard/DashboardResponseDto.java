package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 대시보드 전체 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private SalesSummaryCardDto salesSummary;           // 매출 현황
    private InventorySummaryCardDto inventorySummary;   // 재고 현황
    private EmployeeSummaryCardDto employeeSummary;     // 직원 현황
    private OrderSummaryCardDto orderSummary;           // 주문 현황
    private SalesTrendCardDto salesTrend;               // 매출 추이
    private CategorySalesCardDto categorySales;         // 재고 분석 (카테고리별 매출)
    private AttendanceSummaryCardDto attendanceSummary; // 출근 현황
}

