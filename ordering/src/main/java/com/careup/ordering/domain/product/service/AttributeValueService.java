package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.AttributeValueDto;
import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.repository.AttributeTypeRepository;
import com.careup.ordering.domain.product.repository.AttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 속성 값 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeValueService {
    
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeTypeRepository attributeTypeRepository;
    
    /**
     * 속성 값 생성
     */
    @Transactional
    public AttributeValueDto.Response createAttributeValue(AttributeValueDto.Request request) {
        AttributeType attributeType = attributeTypeRepository.findById(request.getAttributeTypeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + request.getAttributeTypeId()));
        
        // 중복 확인
        if (attributeValueRepository.existsByAttributeTypeAndValue(attributeType, request.getValue())) {
            throw new IllegalArgumentException("이미 존재하는 속성 값입니다: " + request.getValue());
        }
        
        AttributeValue attributeValue = request.toEntity(attributeType);
        AttributeValue saved = attributeValueRepository.save(attributeValue);
        
        return AttributeValueDto.Response.from(saved);
    }
    
    /**
     * 속성 값 일괄 생성
     */
    @Transactional
    public List<AttributeValueDto.Response> createAttributeValuesBulk(AttributeValueDto.BulkCreateRequest request) {
        AttributeType attributeType;
        
        // attributeTypeId가 있으면 사용, 없으면 이름으로 찾기
        if (request.getAttributeTypeId() != null) {
            attributeType = attributeTypeRepository.findById(request.getAttributeTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입입니다: " + request.getAttributeTypeId()));
        } else if (request.getAttributeTypeName() != null) {
            attributeType = attributeTypeRepository.findByName(request.getAttributeTypeName())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 타입 이름입니다: " + request.getAttributeTypeName()));
        } else {
            throw new IllegalArgumentException("attributeTypeId 또는 attributeTypeName 중 하나는 필수입니다.");
        }
        
        List<AttributeValue> attributeValues = request.getValues().stream()
                .map(item -> {
                    // 중복 확인
                    if (attributeValueRepository.existsByAttributeTypeAndValue(attributeType, item.getValue())) {
                        throw new IllegalArgumentException("이미 존재하는 속성 값입니다: " + item.getValue());
                    }
                    
                    return AttributeValue.builder()
                            .attributeType(attributeType)
                            .value(item.getValue())
                            .displayName(item.getDisplayName() != null ? item.getDisplayName() : item.getValue())
                            .displayOrder(item.getDisplayOrder() != null ? item.getDisplayOrder() : 0)
                            .isActive(true)
                            .build();
                })
                .collect(Collectors.toList());
        
        List<AttributeValue> saved = attributeValueRepository.saveAll(attributeValues);
        
        return saved.stream()
                .map(AttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 속성 값 수정
     */
    @Transactional
    public AttributeValueDto.Response updateAttributeValue(Long id, AttributeValueDto.Request request) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + id));
        
        // 값 변경 시 중복 확인
        if (request.getValue() != null && !request.getValue().equals(attributeValue.getValue())) {
            if (attributeValueRepository.existsByAttributeTypeAndValue(attributeValue.getAttributeType(), request.getValue())) {
                throw new IllegalArgumentException("이미 존재하는 속성 값입니다: " + request.getValue());
            }
        }
        
        attributeValue.updateInfo(
                request.getValue(),
                request.getDisplayName(),
                request.getDisplayOrder(),
                request.getIsActive()
        );
        
        return AttributeValueDto.Response.from(attributeValue);
    }
    
    /**
     * 속성 값 삭제
     */
    @Transactional
    public void deleteAttributeValue(Long id) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + id));
        
        attributeValueRepository.delete(attributeValue);
    }
    
    /**
     * 속성 값 활성화/비활성화
     */
    @Transactional
    public AttributeValueDto.Response toggleAttributeValue(Long id) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + id));
        
        attributeValue.toggleActive();
        
        return AttributeValueDto.Response.from(attributeValue);
    }
    
    /**
     * 속성 타입별 속성 값 조회
     */
    public List<AttributeValueDto.Response> getAttributeValuesByType(Long attributeTypeId) {
        return attributeValueRepository.findByAttributeTypeId(attributeTypeId).stream()
                .map(AttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 속성 타입별 활성 속성 값만 조회
     */
    public List<AttributeValueDto.Response> getActiveAttributeValuesByType(Long attributeTypeId) {
        return attributeValueRepository.findActiveByAttributeTypeId(attributeTypeId).stream()
                .map(AttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 속성 값 단건 조회
     */
    public AttributeValueDto.Response getAttributeValue(Long id) {
        AttributeValue attributeValue = attributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + id));
        
        return AttributeValueDto.Response.from(attributeValue);
    }
}

