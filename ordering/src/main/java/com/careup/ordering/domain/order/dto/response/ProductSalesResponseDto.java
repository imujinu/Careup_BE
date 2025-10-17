package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.ProductSalesDto;
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
