package com.careup.branch.domain.chat.dto.sales;

import lombok.Data;

import java.util.List;

@Data
public class SalesLaborInsightDto {
    private Long branchId;
    private Summary summary;
    private List<HourlyDetail> hourlyDetails;

    @Data
    public static class Summary {
        private Integer highestCostHour;
        private Double highestCostRatio;
        private Integer lowestCostHour;
        private Double lowestCostRatio;
        private Double avgCostRatioChange;
        private String message;
    }

    @Data
    public static class HourlyDetail { // ✅ 추가
        private String period;       // 예: "morning", "lunch", "evening"
        private Double avgSales;
        private Double avgLaborCost;
        private Double avgRatio;
    }
}