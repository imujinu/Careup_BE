package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
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
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductAttributeResponseDto> attributes;

    /**
     * Entity -> DTO 변환
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
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
