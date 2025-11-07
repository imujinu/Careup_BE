package com.careup.ordering.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSalesSummaryDto {
    private Long branchId;
    private String branchName;
    private Long totalSales; // 총 매출액
    private Long totalOrders; // 총 주문 수
    private Long averageOrderAmount; // 평균 주문 금액
}

