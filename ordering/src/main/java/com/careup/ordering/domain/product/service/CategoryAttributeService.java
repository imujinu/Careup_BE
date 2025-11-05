package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.CategoryAttributeDto;
import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.CategoryAttribute;
import com.careup.ordering.domain.product.repository.AttributeTypeRepository;
import com.careup.ordering.domain.product.repository.CategoryAttributeRepository;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리별 속성 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryAttributeService {
    
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeTypeRepository attributeTypeRepository;
    
    /**
     * 카테고리에 속성 타입 추가
     */
    @Transactional
    public CategoryAttributeDto.Response addAttributeToCategory(CategoryAttributeDto.Request request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + request.getCategoryId()));
        
        AttributeType attributeType = attributeTypeRepository.findById(request.getAttributeTypeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + request.getAttributeTypeId()));
        
        // 중복 확인
        if (categoryAttributeRepository.existsByCategoryAndAttributeType(category, attributeType)) {
            throw new IllegalArgumentException("이미 해당 카테고리에 추가된 속성 타입입니다.");
        }
        
        CategoryAttribute categoryAttribute = request.toEntity(category, attributeType);
        CategoryAttribute saved = categoryAttributeRepository.save(categoryAttribute);
        
        return CategoryAttributeDto.Response.from(saved);
    }
    
    /**
     * 카테고리 속성 수정
     */
    @Transactional
    public CategoryAttributeDto.Response updateCategoryAttribute(Long id, CategoryAttributeDto.Request request) {
        CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 속성입니다: " + id));
        
        categoryAttribute.updateInfo(request.getIsRequired(), request.getDisplayOrder());
        
        return CategoryAttributeDto.Response.from(categoryAttribute);
    }
    
    /**
     * 카테고리 속성 삭제
     */
    @Transactional
    public void removeAttributeFromCategory(Long id) {
        CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 속성입니다: " + id));
        
        categoryAttributeRepository.delete(categoryAttribute);
    }
    
    /**
     * 카테고리별 속성 목록 조회 (값 포함)
     */
    public List<CategoryAttributeDto.Response> getCategoryAttributesWithValues(Long categoryId) {
        return categoryAttributeRepository.findByCategoryIdWithValues(categoryId).stream()
                .map(CategoryAttributeDto.Response::fromWithValues)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 속성 목록 조회 (값 제외)
     */
    public List<CategoryAttributeDto.Response> getCategoryAttributes(Long categoryId) {
        return categoryAttributeRepository.findByCategoryIdWithAttributeType(categoryId).stream()
                .map(CategoryAttributeDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 필수 속성 목록 조회
     */
    public List<CategoryAttributeDto.Response> getRequiredCategoryAttributes(Long categoryId) {
        return categoryAttributeRepository.findRequiredByCategoryId(categoryId).stream()
                .map(CategoryAttributeDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리 속성 단건 조회
     */
    public CategoryAttributeDto.Response getCategoryAttribute(Long id) {
        CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 속성입니다: " + id));
        
        return CategoryAttributeDto.Response.from(categoryAttribute);
    }
}

