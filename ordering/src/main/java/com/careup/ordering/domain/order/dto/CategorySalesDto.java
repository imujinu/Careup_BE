package com.careup.ordering.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySalesDto {
    private Long categoryId;
    private String categoryName;
    private Long totalSales; // 총 매출액
    private Long totalQuantity; // 총 판매 수량
    private Long orderCount; // 주문 횟수
    private Double percentage; // 전체 대비 비율 (%)
}

