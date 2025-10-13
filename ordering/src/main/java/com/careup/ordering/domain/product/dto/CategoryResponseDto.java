package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    private Long categoryId;
    private String name;
    private String description;
    private Long productCount;

    /**
     * Entity -> DTO 변환
     */
    public static CategoryResponseDto from(Category category) {
        return CategoryResponseDto.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount((long) category.getProducts().size())
                .build();
    }
}
