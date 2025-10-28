package com.careup.branch.domain.chat.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaborCostRequestDto {
    private LocalDate date;             // 오늘 날짜
    private int currentHour;            // 현재 시각
    private long salesSoFar;            // 현재까지 매출
    private int currentStaffCount;      // 현재 근무 직원 수
    private long totalLaborCostSoFar;   // 현재까지 인건비
    private List<HourlyLaborCost> recentWeekData; // 최근 7일 동시간대 데이터

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyLaborCost {
        private LocalDate date;
        private int hour;
        private long sales;
        private long laborCost;
        private int staffCount;
    }
}
