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
    private String branchName;
    private LocalDate forecastDate;
    private Long expectedSales;
    private Long previousPeriodSales;
    private Double growthRate;
    private String forecastBasis; // 예측 근거

    // 엑셀 내보내기용 추가 필드
    private Long pastSales; // 과거 매출
    private Long forecastedSales; // 예측 매출
    private Integer forecastDays; // 예측 기간(일)
}

