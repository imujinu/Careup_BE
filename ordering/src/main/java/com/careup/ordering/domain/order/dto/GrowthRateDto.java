package com.careup.ordering.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증가율 정보를 담는 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthRateDto {
    private Double totalSalesGrowth; // 총 매출 증가율 (%)
    private Double monthlySalesGrowth; // 월간 매출 증가율 (%)
    private Double totalOrdersGrowth; // 총 주문 수 증가율 (%)
}

