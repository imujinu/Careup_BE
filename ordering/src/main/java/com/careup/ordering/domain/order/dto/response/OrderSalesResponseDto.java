package com.careup.ordering.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSalesResponseDto {
    private Long branchId;
    private String applicableMonth;
    private Long totalSales;
}

