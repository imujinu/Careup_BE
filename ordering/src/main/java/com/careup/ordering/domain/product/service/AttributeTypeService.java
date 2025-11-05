package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.AttributeTypeDto;
import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.repository.AttributeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 속성 타입 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeTypeService {
    
    private final AttributeTypeRepository attributeTypeRepository;
    
    /**
     * 속성 타입 생성
     */
    @Transactional
    public AttributeTypeDto.Response createAttributeType(AttributeTypeDto.Request request) {
        // 중복 확인
        if (attributeTypeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 속성 타입 이름입니다: " + request.getName());
        }
        
        AttributeType attributeType = request.toEntity();
        AttributeType saved = attributeTypeRepository.save(attributeType);
        
        return AttributeTypeDto.Response.from(saved);
    }
    
    /**
     * 속성 타입 수정
     */
    @Transactional
    public AttributeTypeDto.Response updateAttributeType(Long id, AttributeTypeDto.Request request) {
        AttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + id));
        
        // 이름 변경 시 중복 확인
        if (request.getName() != null && !request.getName().equals(attributeType.getName())) {
            if (attributeTypeRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("이미 존재하는 속성 타입 이름입니다: " + request.getName());
            }
        }
        
        attributeType.updateInfo(
            request.getName(),
            request.getDescription(),
            request.getIsRequired(),
            request.getDisplayOrder()
        );
        
        return AttributeTypeDto.Response.from(attributeType);
    }
    
    /**
     * 속성 타입 삭제
     */
    @Transactional
    public void deleteAttributeType(Long id) {
        AttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + id));
        
        attributeTypeRepository.delete(attributeType);
    }
    
    /**
     * 속성 타입 단건 조회
     */
    public AttributeTypeDto.Response getAttributeType(Long id) {
        AttributeType attributeType = attributeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + id));
        
        return AttributeTypeDto.Response.from(attributeType);
    }
    
    /**
     * 속성 타입 전체 조회 (값 포함)
     */
    public List<AttributeTypeDto.Response> getAllAttributeTypesWithValues() {
        return attributeTypeRepository.findAllWithValues().stream()
                .map(AttributeTypeDto.Response::fromWithValues)
                .collect(Collectors.toList());
    }
    
    /**
     * 속성 타입 전체 조회 (값 제외)
     */
    public List<AttributeTypeDto.Response> getAllAttributeTypes() {
        return attributeTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(AttributeTypeDto.Response::from)
                .collect(Collectors.toList());
    }
}
