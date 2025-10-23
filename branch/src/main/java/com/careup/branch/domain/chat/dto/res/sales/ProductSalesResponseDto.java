package com.careup.branch.domain.chat.dto.res.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesResponseDto {
    private Long branchId;
    private String sortType; // HIGH_MARGIN, LOW_MARGIN, HIGH_SALES, LOW_SALES
    private List<ProductSalesDto> products;
}
