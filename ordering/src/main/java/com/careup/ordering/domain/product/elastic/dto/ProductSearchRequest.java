package com.careup.ordering.domain.product.elastic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private Long minPrice;
    private Long maxPrice;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;
}

