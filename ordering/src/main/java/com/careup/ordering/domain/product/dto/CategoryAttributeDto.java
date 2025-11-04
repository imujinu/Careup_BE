package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.CategoryAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 속성 DTO
 */
public class CategoryAttributeDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long categoryId;
        private Long attributeTypeId;
        private Boolean isRequired;
        private Integer displayOrder;
        
        public CategoryAttribute toEntity(Category category, AttributeType attributeType) {
            return CategoryAttribute.builder()
                    .category(category)
                    .attributeType(attributeType)
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
        private Long categoryId;
        private String categoryName;
        private Long attributeTypeId;
        private String attributeTypeName;
        private Boolean isRequired;
        private Integer displayOrder;
        private List<AttributeValueDto.Response> availableValues;
        
        public static Response from(CategoryAttribute categoryAttribute) {
            return Response.builder()
                    .id(categoryAttribute.getId())
                    .categoryId(categoryAttribute.getCategory().getId())
                    .categoryName(categoryAttribute.getCategory().getName())
                    .attributeTypeId(categoryAttribute.getAttributeType().getId())
                    .attributeTypeName(categoryAttribute.getAttributeType().getName())
                    .isRequired(categoryAttribute.getIsRequired())
                    .displayOrder(categoryAttribute.getDisplayOrder())
                    .build();
        }
        
        public static Response fromWithValues(CategoryAttribute categoryAttribute) {
            return Response.builder()
                    .id(categoryAttribute.getId())
                    .categoryId(categoryAttribute.getCategory().getId())
                    .categoryName(categoryAttribute.getCategory().getName())
                    .attributeTypeId(categoryAttribute.getAttributeType().getId())
                    .attributeTypeName(categoryAttribute.getAttributeType().getName())
                    .isRequired(categoryAttribute.getIsRequired())
                    .displayOrder(categoryAttribute.getDisplayOrder())
                    .availableValues(categoryAttribute.getAttributeType().getAttributeValues().stream()
                            .filter(av -> av.getIsActive())
                            .map(AttributeValueDto.Response::from)
                            .collect(Collectors.toList()))
                    .build();
        }
    }
}
