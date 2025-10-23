package com.careup.branch.domain.chat.dto.res.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPredictionResponseDto {
    private LocalDate date;               // 오늘 날짜
    private long totalSales;              // 오늘 예상 총 매출
    private long salesSoFar;              // 현재까지 매출
    private double remainingHoursSales;   // 남은 시간 예상 매출
    private double percentChangePrevDay;  // 전일 대비 % 변화
    private double percentChangePrevWeek; // 전주 동일 요일 대비 % 변화
    private String analysis;              // AI가 제공하는 간단한 분석 메시지
}
