package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
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

    /**
     * Entity → DTO 변환 정적 메서드
     */
    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .productId(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .description(product.getDescription())
                .supplyPrice(product.getSupplyPrice())
                .minPrice(product.getMinPrice())
                .maxPrice(product.getMaxPrice())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus().toString())
                .visibility(product.getVisibility().toString())
                .build();
    }
}
