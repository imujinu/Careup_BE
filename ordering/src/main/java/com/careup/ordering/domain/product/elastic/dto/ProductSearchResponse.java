package com.careup.ordering.domain.product.elastic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResponse {
    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private Long price;
    private String imageUrl;
    private String highlightedName; // 하이라이팅된 상품명
}

