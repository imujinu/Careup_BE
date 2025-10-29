package com.careup.branch.domain.chat.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesDto {
    private Long productId;
    private String productName;
    private Long totalQuantity; // 총 판매 수량
    private Long totalSales; // 총 매출액
    private Long supplyPrice; // 공급가
    private Long averageSellingPrice; // 평균 판매가
    private Double marginRate; // 마진율
    private Long orderCount; // 주문 횟수
}

