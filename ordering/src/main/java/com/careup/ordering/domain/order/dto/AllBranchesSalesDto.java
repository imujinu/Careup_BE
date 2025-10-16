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
public class AllBranchesSalesDto {
    private LocalDate date;
    private String period; // DAY, WEEK, MONTH
    private Long totalSales; // 전체 지점 총 매출
    private Long totalOrders; // 전체 지점 총 주문 수
    private Long averageOrderAmount; // 평균 주문 금액
    private Integer activeBranchCount; // 활성 지점 수
    private Long averageSalesPerBranch; // 지점당 평균 매출
}

