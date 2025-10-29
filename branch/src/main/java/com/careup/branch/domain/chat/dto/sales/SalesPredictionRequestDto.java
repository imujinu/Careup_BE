package com.careup.branch.domain.chat.dto.sales;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPredictionRequestDto {
    private LocalDate today;              // 오늘 날짜
    private DayOfWeek dayOfWeek;          // 오늘 요일
    private int currentHour;
    private int closingHour;
    private long todaySales;         // 현재까지 누적 매출
    private List<HourlySales> recentHourlySales; // 최근 7일 시간대별 매출

    @Getter
    public class HourlySales {
        private LocalDate date;  // 날짜
        private int hour;        // 시간대 (0~23)
        private long sales;      // 매출액
    }

    public void updateDto(LocalDate today, DayOfWeek dayOfWeek, int currentHour, int closingHour){
        this.today = today;
        this.dayOfWeek = dayOfWeek;
        this.currentHour = currentHour;
        this.closingHour = closingHour;
    }
}
