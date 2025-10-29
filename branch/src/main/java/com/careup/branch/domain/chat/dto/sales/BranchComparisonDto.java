package com.careup.branch.domain.chat.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchComparisonDto {
    private Long branchId;
    private String branchName;
    private Long totalSales;
    private Long totalOrders;
    private Long averageOrderAmount;
    private Double salesGrowthRate; // 매출 성장률
}

