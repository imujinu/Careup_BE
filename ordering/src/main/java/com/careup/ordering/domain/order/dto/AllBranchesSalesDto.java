package com.careup.ordering.domain.order.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AllBranchesSalesDto {
    private LocalDate date;
    private String period; // DAY, WEEK, MONTH
    private Long totalSales; // 전체 지점 총 매출
    private Long totalOrders; // 전체 지점 총 주문 수
    private Long averageOrderAmount; // 평균 주문 금액
    private Long activeBranchCount; // 활성 지점 수
    private Long averageSalesPerBranch; // 지점당 평균 매출

    @QueryProjection
    public AllBranchesSalesDto(
            String date,
            String period,
            Long totalSales,
            Long totalOrders,
            Long averageOrderAmount,
            Long activeBranchCount,
            Long averageSalesPerBranch
    ) {
        this.date = LocalDate.parse(date);
        this.period = period;
        this.totalSales = totalSales;
        this.totalOrders = totalOrders;
        this.averageOrderAmount = averageOrderAmount;
        this.activeBranchCount = activeBranchCount;
        this.averageSalesPerBranch = averageSalesPerBranch;
    }

}

