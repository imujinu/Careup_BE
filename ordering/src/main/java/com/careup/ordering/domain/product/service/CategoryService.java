package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.CategoryRequestDto;
import com.careup.ordering.domain.product.dto.CategoryResponseDto;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;


    // 카테고리 등록
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return convertToCategoryResponse(savedCategory);
    }

    // 카테고리 목록 조회
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    // dto 변환 메서드
    private CategoryResponseDto convertToCategoryResponse(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
