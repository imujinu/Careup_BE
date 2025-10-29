package com.careup.branch.domain.chat.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportDto {
    private Long branchId;
    private Summary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private List<String> topHours;
        private String weekChange;
        private String monthChange;
        private String topMarginProduct;
        private String lowMarginProduct;
        private String avgLaborRatio;
        private String nextWeekForecast;
        private String message;
    }
}
