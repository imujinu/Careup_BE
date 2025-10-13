package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.CategoryDto;
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
    public CategoryDto.Response createCategory(CategoryDto.Request request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return convertToCategoryResponse(savedCategory);
    }

    // 카테고리 목록 조회
    @Transactional(readOnly = true)
    public List<CategoryDto.Response> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    // dto 변환 메서드
    private CategoryDto.Response convertToCategoryResponse(Category category) {
        return new CategoryDto.Response(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
