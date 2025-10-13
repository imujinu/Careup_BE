package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long productId;
    private String name;
    private String description;
    private Long supplyPrice;
    private Long minPrice;
    private Long maxPrice;
    private String imageUrl;
    private String status;
    private String visibility;
}

