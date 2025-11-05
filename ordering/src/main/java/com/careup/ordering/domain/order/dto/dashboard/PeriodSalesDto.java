package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기간별 매출 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSalesDto {
    private String periodLabel;  // "2025-01", "Week 1" 등
    private Long sales;
}

