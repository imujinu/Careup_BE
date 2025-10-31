package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 매출 추이 카드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendCardDto {
    private String period;             // "YEARLY", "MONTHLY", "WEEKLY"
    private List<PeriodSalesDto> salesData;  // 기간별 매출 데이터 (차트용)
    private Long totalSales;           // 올해 총 매출
    private Double yearOverYearGrowth; // 전년 대비 증감률 (%)
    private Double goalAchievementRate; // 목표 달성률 (%)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodSalesDto {
        private String periodLabel;  // "2025-01", "Week 1" 등
        private Long sales;
    }
}

