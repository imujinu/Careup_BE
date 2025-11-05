package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttributeValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상품 속성 값 DTO
 */
public class ProductAttributeValueDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long productId;
        private Long attributeValueId;
        private String customValue; // "기타" 선택 시
        
        public ProductAttributeValue toEntity(Product product, AttributeValue attributeValue) {
            return ProductAttributeValue.builder()
                    .product(product)
                    .attributeValue(attributeValue)
                    .customValue(customValue)
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long productId;
        private Long attributeValueId;
        private String attributeTypeName;
        private String value;
        private String displayName;
        private String customValue;
        
        public static Response from(ProductAttributeValue productAttributeValue) {
            AttributeValue av = productAttributeValue.getAttributeValue();
            return Response.builder()
                    .id(productAttributeValue.getId())
                    .productId(productAttributeValue.getProduct().getId())
                    .attributeValueId(av.getId())
                    .attributeTypeName(av.getAttributeType().getName())
                    .value(av.getValue())
                    .displayName(av.getDisplayName())
                    .customValue(productAttributeValue.getCustomValue())
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkCreateRequest {
        private Long productId;
        private List<AttributeSelection> attributes;
        
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AttributeSelection {
            private Long attributeValueId;
            private String customValue; // "기타" 선택 시
        }
    }
}
