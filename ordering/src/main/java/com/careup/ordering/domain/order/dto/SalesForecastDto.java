package com.careup.ordering.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesForecastDto {
    private Long branchId;
    private LocalDate forecastDate;
    private Long expectedSales;
    private Long previousPeriodSales;
    private Double growthRate;
    private String forecastBasis; // 예측 근거
}

