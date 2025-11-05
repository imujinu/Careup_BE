package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.CategoryAttributeDto;
import com.careup.ordering.domain.product.service.CategoryAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리별 속성 관리 컨트롤러
 * 특정 카테고리에 어떤 속성 타입을 사용할지 정의
 */
@Slf4j
@RestController
@RequestMapping("/api/category-attributes")
@RequiredArgsConstructor
public class CategoryAttributeController {
    
    private final CategoryAttributeService categoryAttributeService;
    
    /**
     * 카테고리에 속성 타입 추가
     */
    @PostMapping
    public ResponseEntity<ResponseDto<CategoryAttributeDto.Response>> addAttributeToCategory(
            @Valid @RequestBody CategoryAttributeDto.Request request) {
        log.info("POST /api/category-attributes - 카테고리 속성 추가: categoryId={}, attributeTypeId={}", 
                request.getCategoryId(), request.getAttributeTypeId());
        
        CategoryAttributeDto.Response response = categoryAttributeService.addAttributeToCategory(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 카테고리 속성 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<CategoryAttributeDto.Response>> updateCategoryAttribute(
            @PathVariable Long id,
            @Valid @RequestBody CategoryAttributeDto.Request request) {
        log.info("PUT /api/category-attributes/{} - 카테고리 속성 수정", id);
        
        CategoryAttributeDto.Response response = categoryAttributeService.updateCategoryAttribute(id, request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 카테고리 속성 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> removeAttributeFromCategory(@PathVariable Long id) {
        log.info("DELETE /api/category-attributes/{} - 카테고리 속성 삭제", id);
        
        categoryAttributeService.removeAttributeFromCategory(id);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 카테고리 속성 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<CategoryAttributeDto.Response>> getCategoryAttribute(@PathVariable Long id) {
        log.info("GET /api/category-attributes/{} - 카테고리 속성 조회", id);
        
        CategoryAttributeDto.Response response = categoryAttributeService.getCategoryAttribute(id);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 카테고리별 속성 목록 조회 (값 포함)
     */
    @GetMapping("/category/{categoryId}/with-values")
    public ResponseEntity<ResponseDto<List<CategoryAttributeDto.Response>>> getCategoryAttributesWithValues(
            @PathVariable Long categoryId) {
        log.info("GET /api/category-attributes/category/{}/with-values - 카테고리별 속성 목록 조회 (값 포함)", categoryId);
        
        List<CategoryAttributeDto.Response> response = categoryAttributeService.getCategoryAttributesWithValues(categoryId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 카테고리별 속성 목록 조회 (값 제외)
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ResponseDto<List<CategoryAttributeDto.Response>>> getCategoryAttributes(
            @PathVariable Long categoryId) {
        log.info("GET /api/category-attributes/category/{} - 카테고리별 속성 목록 조회", categoryId);
        
        List<CategoryAttributeDto.Response> response = categoryAttributeService.getCategoryAttributes(categoryId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 카테고리별 필수 속성 목록 조회
     */
    @GetMapping("/category/{categoryId}/required")
    public ResponseEntity<ResponseDto<List<CategoryAttributeDto.Response>>> getRequiredCategoryAttributes(
            @PathVariable Long categoryId) {
        log.info("GET /api/category-attributes/category/{}/required - 카테고리별 필수 속성 조회", categoryId);
        
        List<CategoryAttributeDto.Response> response = categoryAttributeService.getRequiredCategoryAttributes(categoryId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}

