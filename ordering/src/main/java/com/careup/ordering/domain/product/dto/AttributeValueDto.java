package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.AttributeValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 속성 값 DTO
 */
public class AttributeValueDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long attributeTypeId;
        private String displayName;
        private Integer displayOrder;
        private Boolean isActive;
        
        public AttributeValue toEntity(AttributeType attributeType) {
            return AttributeValue.builder()
                    .attributeType(attributeType)
                    .displayName(displayName)
                    .displayOrder(displayOrder)
                    .isActive(isActive)
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long attributeTypeId;
        private String attributeTypeName;
        private String displayName;
        private Integer displayOrder;
        private Boolean isActive;
        
        public static Response from(AttributeValue attributeValue) {
            return Response.builder()
                    .id(attributeValue.getId())
                    .attributeTypeId(attributeValue.getAttributeType().getId())
                    .attributeTypeName(attributeValue.getAttributeType().getName())
                    .displayName(attributeValue.getDisplayName())
                    .displayOrder(attributeValue.getDisplayOrder())
                    .isActive(attributeValue.getIsActive())
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkCreateRequest {
        private Long attributeTypeId;
        private String attributeTypeName; // 타입 이름으로도 생성 가능
        private List<ValueItem> values;
        
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ValueItem {
            private String displayName;
            private Integer displayOrder;
        }
    }
}
