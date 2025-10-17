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
public class BranchSalesDetailDto {
    private Long branchId;
    private String branchName;
    private LocalDate date;
    private String period; // DAY, WEEK, MONTH
    private Long totalSales;
    private Long totalOrders;
    private Long averageOrderAmount;
    private Double marketShare; // 전체 대비 점유율 (%)
    private Integer ranking; // 매출 순위
}

