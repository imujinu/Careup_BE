package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.ProductAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeResponseDto {
    private Long attributeId;
    private Long productId;
    private String attributeName;   // 색상, 사이즈
    private String attributeValue;  // 빨강, L
    
    public static ProductAttributeResponseDto from(ProductAttribute attribute) {
        return ProductAttributeResponseDto.builder()
                .attributeId(attribute.getId())
                .productId(attribute.getProduct().getId())
                .attributeName(attribute.getAttributeName())
                .attributeValue(attribute.getAttributeValue())
                .build();
    }
}
