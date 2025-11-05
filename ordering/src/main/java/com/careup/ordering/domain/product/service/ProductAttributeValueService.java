package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.ProductAttributeValueDto;
import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttributeValue;
import com.careup.ordering.domain.product.repository.AttributeValueRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeValueRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 속성 값 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductAttributeValueService {
    
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final ProductRepository productRepository;
    private final AttributeValueRepository attributeValueRepository;
    
    /**
     * 상품에 속성 값 추가
     */
    @Transactional
    public ProductAttributeValueDto.Response addAttributeValueToProduct(ProductAttributeValueDto.Request request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다: " + request.getProductId()));
        
        AttributeValue attributeValue = attributeValueRepository.findById(request.getAttributeValueId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + request.getAttributeValueId()));
        
        // 중복 확인
        if (productAttributeValueRepository.existsByProductAndAttributeValue(product, attributeValue)) {
            throw new IllegalArgumentException("이미 해당 상품에 추가된 속성 값입니다.");
        }
        
        ProductAttributeValue productAttributeValue = request.toEntity(product, attributeValue);
        ProductAttributeValue saved = productAttributeValueRepository.save(productAttributeValue);
        
        // 저장 후 JOIN FETCH로 연관 엔티티 포함하여 다시 조회
        ProductAttributeValue loaded = productAttributeValueRepository.findByIdWithDetails(saved.getId())
                .orElseThrow(() -> new IllegalArgumentException("저장된 상품 속성 값을 찾을 수 없습니다."));
        
        return ProductAttributeValueDto.Response.from(loaded);
    }
    
    /**
     * 상품 속성 값 일괄 추가
     */
    @Transactional
    public List<ProductAttributeValueDto.Response> addAttributeValuesToProduct(ProductAttributeValueDto.BulkCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다: " + request.getProductId()));
        
        // 기존 속성 값 삭제 (전체 교체)
        productAttributeValueRepository.deleteByProductId(request.getProductId());
        
        List<ProductAttributeValue> productAttributeValues = request.getAttributes().stream()
                .map(attr -> {
                    if (attr.getAttributeValueId() == null) {
                        throw new IllegalArgumentException("속성 값 ID는 필수입니다.");
                    }
                    
                    AttributeValue attributeValue = attributeValueRepository.findById(attr.getAttributeValueId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 속성 값입니다: " + attr.getAttributeValueId()));
                    
                    return ProductAttributeValue.builder()
                            .product(product)
                            .attributeValue(attributeValue)
                            .customValue(attr.getCustomValue())
                            .build();
                })
                .collect(Collectors.toList());
        
        productAttributeValueRepository.saveAll(productAttributeValues);
        
        // 저장 후 JOIN FETCH로 연관 엔티티 포함하여 다시 조회
        List<ProductAttributeValue> loaded = productAttributeValueRepository.findByProductIdWithDetails(request.getProductId());
        
        return loaded.stream()
                .map(ProductAttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 상품 속성 값 수정
     */
    @Transactional
    public ProductAttributeValueDto.Response updateProductAttributeValue(Long id, String customValue) {
        ProductAttributeValue productAttributeValue = productAttributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 속성 값입니다: " + id));
        
        productAttributeValue.updateCustomValue(customValue);
        
        // 저장 후 JOIN FETCH로 연관 엔티티 포함하여 다시 조회
        ProductAttributeValue loaded = productAttributeValueRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("수정된 상품 속성 값을 찾을 수 없습니다."));
        
        return ProductAttributeValueDto.Response.from(loaded);
    }
    
    /**
     * 상품 속성 값 삭제
     */
    @Transactional
    public void removeAttributeValueFromProduct(Long id) {
        ProductAttributeValue productAttributeValue = productAttributeValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 속성 값입니다: " + id));
        
        productAttributeValueRepository.delete(productAttributeValue);
    }
    
    /**
     * 상품의 모든 속성 값 삭제
     */
    @Transactional
    public void removeAllAttributeValuesFromProduct(Long productId) {
        productAttributeValueRepository.deleteByProductId(productId);
    }
    
    /**
     * 상품의 모든 속성 값 조회
     */
    public List<ProductAttributeValueDto.Response> getProductAttributeValues(Long productId) {
        return productAttributeValueRepository.findByProductIdWithDetails(productId).stream()
                .map(ProductAttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 상품의 특정 속성 타입 값 조회
     */
    public List<ProductAttributeValueDto.Response> getProductAttributeValuesByType(Long productId, Long attributeTypeId) {
        return productAttributeValueRepository.findByProductIdAndAttributeTypeId(productId, attributeTypeId).stream()
                .map(ProductAttributeValueDto.Response::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 상품 속성 값 단건 조회
     */
    public ProductAttributeValueDto.Response getProductAttributeValue(Long id) {
        ProductAttributeValue productAttributeValue = productAttributeValueRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 속성 값입니다: " + id));
        
        return ProductAttributeValueDto.Response.from(productAttributeValue);
    }
}

