package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.ProductAttributeValueDto;
import com.careup.ordering.domain.product.service.ProductAttributeValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 속성 값 관리 컨트롤러
 * 상품이 실제로 선택한 속성 값들 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/product-attribute-values")
@RequiredArgsConstructor
public class ProductAttributeValueController {
    
    private final ProductAttributeValueService productAttributeValueService;
    
    /**
     * 상품에 속성 값 추가
     */
    @PostMapping
    public ResponseEntity<ResponseDto<ProductAttributeValueDto.Response>> addAttributeValueToProduct(
            @Valid @RequestBody ProductAttributeValueDto.Request request) {
        log.info("POST /api/product-attribute-values - 상품 속성 값 추가: productId={}, attributeValueId={}", 
                request.getProductId(), request.getAttributeValueId());
        
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (request.getAttributeValueId() == null) {
            throw new IllegalArgumentException("attributeValueId는 필수입니다.");
        }
        
        ProductAttributeValueDto.Response response = productAttributeValueService.addAttributeValueToProduct(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 상품에 속성 값 일괄 추가 (전체 교체)
     */
    @PostMapping("/bulk")
    public ResponseEntity<ResponseDto<List<ProductAttributeValueDto.Response>>> addAttributeValuesToProduct(
            @Valid @RequestBody ProductAttributeValueDto.BulkCreateRequest request) {
        log.info("POST /api/product-attribute-values/bulk - 상품 속성 값 일괄 추가: productId={}, count={}", 
                request.getProductId(), request.getAttributes() != null ? request.getAttributes().size() : 0);
        
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        if (request.getAttributes() == null || request.getAttributes().isEmpty()) {
            throw new IllegalArgumentException("attributes는 필수이며 최소 1개 이상이어야 합니다.");
        }
        
        List<ProductAttributeValueDto.Response> response = productAttributeValueService.addAttributeValuesToProduct(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 상품 속성 값 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductAttributeValueDto.Response>> updateProductAttributeValue(
            @PathVariable Long id,
            @RequestBody(required = false) String customValue) {
        log.info("PUT /api/product-attribute-values/{} - 상품 속성 값 수정", id);
        
        ProductAttributeValueDto.Response response = productAttributeValueService.updateProductAttributeValue(id, customValue);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품 속성 값 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> removeAttributeValueFromProduct(@PathVariable Long id) {
        log.info("DELETE /api/product-attribute-values/{} - 상품 속성 값 삭제", id);
        
        productAttributeValueService.removeAttributeValueFromProduct(id);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품의 모든 속성 값 삭제
     */
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<ResponseDto<Void>> removeAllAttributeValuesFromProduct(@PathVariable Long productId) {
        log.info("DELETE /api/product-attribute-values/product/{} - 상품의 모든 속성 값 삭제", productId);
        
        productAttributeValueService.removeAllAttributeValuesFromProduct(productId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품 속성 값 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductAttributeValueDto.Response>> getProductAttributeValue(@PathVariable Long id) {
        log.info("GET /api/product-attribute-values/{} - 상품 속성 값 조회", id);
        
        ProductAttributeValueDto.Response response = productAttributeValueService.getProductAttributeValue(id);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품의 모든 속성 값 조회
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ResponseDto<List<ProductAttributeValueDto.Response>>> getProductAttributeValues(
            @PathVariable Long productId) {
        log.info("GET /api/product-attribute-values/product/{} - 상품의 모든 속성 값 조회", productId);
        
        List<ProductAttributeValueDto.Response> response = productAttributeValueService.getProductAttributeValues(productId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품의 특정 속성 타입 값 조회
     */
    @GetMapping("/product/{productId}/type/{attributeTypeId}")
    public ResponseEntity<ResponseDto<List<ProductAttributeValueDto.Response>>> getProductAttributeValuesByType(
            @PathVariable Long productId,
            @PathVariable Long attributeTypeId) {
        log.info("GET /api/product-attribute-values/product/{}/type/{} - 상품의 특정 속성 타입 값 조회", 
                productId, attributeTypeId);
        
        List<ProductAttributeValueDto.Response> response = 
                productAttributeValueService.getProductAttributeValuesByType(productId, attributeTypeId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}

