package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 속성 타입 DTO
 */
public class AttributeTypeDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String name;
        private String description;
        private Boolean isRequired;
        private Integer displayOrder;
        
        public AttributeType toEntity() {
            return AttributeType.builder()
                    .name(name)
                    .description(description)
                    .isRequired(isRequired)
                    .displayOrder(displayOrder)
                    .build();
        }
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private Boolean isRequired;
        private Integer displayOrder;
        private List<AttributeValueDto.Response> values;
        
        public static Response from(AttributeType attributeType) {
            return Response.builder()
                    .id(attributeType.getId())
                    .name(attributeType.getName())
                    .description(attributeType.getDescription())
                    .isRequired(attributeType.getIsRequired())
                    .displayOrder(attributeType.getDisplayOrder())
                    .build();
        }
        
        public static Response fromWithValues(AttributeType attributeType) {
            return Response.builder()
                    .id(attributeType.getId())
                    .name(attributeType.getName())
                    .description(attributeType.getDescription())
                    .isRequired(attributeType.getIsRequired())
                    .displayOrder(attributeType.getDisplayOrder())
                    .values(attributeType.getAttributeValues().stream()
                            .map(AttributeValueDto.Response::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }
}
