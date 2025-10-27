package com.careup.branch.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesStatisticsDto {
    private LocalDate date;
    private String period; // DAY, WEEK, MONTH
    private String dayOfWeek; // 요일별
    private Integer hour; // 시간별
    private Long totalSales; // 총 매출액
    private Long totalOrders; // 총 주문 수
    private Long averageOrderAmount; // 평균 주문 금액
}

