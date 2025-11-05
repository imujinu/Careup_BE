package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.AttributeTypeDto;
import com.careup.ordering.domain.product.service.AttributeTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 속성 타입 관리 컨트롤러
 * 속성 템플릿 관리 (예: 색상, 용도, 계절 등)
 */
@Slf4j
@RestController
@RequestMapping("/api/attribute-types")
@RequiredArgsConstructor
public class AttributeTypeController {
    
    private final AttributeTypeService attributeTypeService;
    
    /**
     * 속성 타입 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto<AttributeTypeDto.Response>> createAttributeType(
            @Valid @RequestBody AttributeTypeDto.Request request) {
        log.info("POST /api/attribute-types - 속성 타입 등록: {}", request.getName());
        
        AttributeTypeDto.Response response = attributeTypeService.createAttributeType(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 속성 타입 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<AttributeTypeDto.Response>> updateAttributeType(
            @PathVariable Long id,
            @Valid @RequestBody AttributeTypeDto.Request request) {
        log.info("PUT /api/attribute-types/{} - 속성 타입 수정", id);
        
        AttributeTypeDto.Response response = attributeTypeService.updateAttributeType(id, request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteAttributeType(@PathVariable Long id) {
        log.info("DELETE /api/attribute-types/{} - 속성 타입 삭제", id);
        
        attributeTypeService.deleteAttributeType(id);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<AttributeTypeDto.Response>> getAttributeType(@PathVariable Long id) {
        log.info("GET /api/attribute-types/{} - 속성 타입 조회", id);
        
        AttributeTypeDto.Response response = attributeTypeService.getAttributeType(id);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입 전체 조회 (값 포함)
     */
    @GetMapping("/with-values")
    public ResponseEntity<ResponseDto<List<AttributeTypeDto.Response>>> getAllAttributeTypesWithValues() {
        log.info("GET /api/attribute-types/with-values - 속성 타입 전체 조회 (값 포함)");
        
        List<AttributeTypeDto.Response> response = attributeTypeService.getAllAttributeTypesWithValues();
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입 전체 조회 (값 제외)
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<AttributeTypeDto.Response>>> getAllAttributeTypes() {
        log.info("GET /api/attribute-types - 속성 타입 전체 조회");
        
        List<AttributeTypeDto.Response> response = attributeTypeService.getAllAttributeTypes();
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}

