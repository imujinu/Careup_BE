package com.careup.ordering.domain.recomendation.dto;

import com.careup.ordering.domain.product.dto.ProductAttributeValueDto;
import com.careup.ordering.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductsResDto {
    private Long productId;
    private String categoryName;
    private String productName;
    private String description;
    private String imageUrl;
    private Long price;
    private Long coPurchaseCount;
    private Long viewCount;
    private List<ProductAttributeValueDto.Response> attributeValues;


    public static ProductsResDto makeDto(Product product){
        String categoryName = null;

        try {
            if (product.getCategory() != null) {
                categoryName = product.getCategory().getName();
            }
        } catch (Exception e) {
            categoryName = "미분류";
        }


        return ProductsResDto.builder()
                .productId(product.getId())
                .categoryName(categoryName)
                .productName(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .price(product.getMaxPrice())
                .viewCount(product.getViewCount())
                .attributeValues(product.getProductAttributeValues() != null ?
                        product.getProductAttributeValues().stream()
                                .map(ProductAttributeValueDto.Response::from)
                                .collect(Collectors.toList()) :
                        List.of())
                .build();
    }
}

