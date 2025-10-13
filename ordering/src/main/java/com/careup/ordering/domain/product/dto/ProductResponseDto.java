package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long productId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private Long supplyPrice;  // 본사 공급가
    private Long minPrice;     // 권장 최소 판매가
    private Long maxPrice;     // 권장 최대 판매가
    private String imageUrl;
    private String status;
    private String visibility;
}
