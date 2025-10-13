package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.CategoryRequestDto;
import com.careup.ordering.domain.product.dto.CategoryResponseDto;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 등록
     */
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        log.info("카테고리 등록 시작 - name: {}", requestDto.getName());

        Category category = Category.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("카테고리 등록 완료 - categoryId: {}", savedCategory.getId());

        return CategoryResponseDto.from(savedCategory);
    }

    /**
     * 전체 카테고리 조회
     */
    public List<CategoryResponseDto> getAllCategories() {
        log.info("전체 카테고리 조회");

        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 단건 조회
     */
    public CategoryResponseDto getCategoryById(Long categoryId) {
        log.info("카테고리 조회 - categoryId: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 카테고리입니다. ID: " + categoryId));

        return CategoryResponseDto.from(category);
    }

    /**
     * 카테고리 삭제
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("카테고리 삭제 - categoryId: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. ID: " + categoryId);
        }

        categoryRepository.deleteById(categoryId);
        log.info("카테고리 삭제 완료 - categoryId: {}", categoryId);
    }
}
