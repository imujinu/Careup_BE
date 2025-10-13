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
    private String attributeName;
    private String attributeValue;

    /**
     * Entity -> DTO 변환
     */
    public static ProductAttributeResponseDto from(ProductAttribute attribute) {
        return ProductAttributeResponseDto.builder()
                .attributeId(attribute.getId())
                .attributeName(attribute.getAttributeName())
                .attributeValue(attribute.getAttributeValue())
                .build();
    }
}
