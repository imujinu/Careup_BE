package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    private Long categoryId;
    private String name;
    private String description;
    private Long supplyPrice;
    private Long minPrice;
    private Long maxPrice;
    private String imageUrl;
    private String visibility;
    // 상품 속성
    private List<ProductAttributeRequestDto> attributes;
}
