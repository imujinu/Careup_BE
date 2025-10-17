package com.careup.ordering.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * branch 모듈의 SalesForecast 응답을 받기 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesForecastResponseDto {
    private Long branchId;
    private String branchName;
    private CurrentForecast currentForecast;
    private List<ForecastHistory> forecastHistory;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentForecast {
        private Long id;
        private Long branchId;
        private String branchName;
        private Long amount;
        private Date periodStart;
        private Date periodEnd;
        private Date createdAt;
        private String forecastBasis;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForecastHistory {
        private Long id;
        private Long branchId;
        private String branchName;
        private Long amount;
        private Date periodStart;
        private Date periodEnd;
        private Date createdAt;
        private String forecastBasis;
    }
}

