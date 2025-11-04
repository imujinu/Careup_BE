package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.AttributeValueDto;
import com.careup.ordering.domain.product.service.AttributeValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 속성 값 관리 컨트롤러
 * 속성 타입별 선택 가능한 값 관리 (예: white, black, 러닝, 헬스 등)
 */
@Slf4j
@RestController
@RequestMapping("/api/attribute-values")
@RequiredArgsConstructor
public class AttributeValueController {
    
    private final AttributeValueService attributeValueService;
    
    /**
     * 속성 값 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto<AttributeValueDto.Response>> createAttributeValue(
            @Valid @RequestBody AttributeValueDto.Request request) {
        log.info("POST /api/attribute-values - 속성 값 등록: attributeTypeId={}, value={}", 
                request.getAttributeTypeId(), request.getValue());
        
        AttributeValueDto.Response response = attributeValueService.createAttributeValue(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 속성 값 일괄 등록
     */
    @PostMapping("/bulk")
    public ResponseEntity<ResponseDto<List<AttributeValueDto.Response>>> createAttributeValuesBulk(
            @Valid @RequestBody AttributeValueDto.BulkCreateRequest request) {
        log.info("POST /api/attribute-values/bulk - 속성 값 일괄 등록: attributeTypeId={}, count={}", 
                request.getAttributeTypeId(), request.getValues().size());
        
        List<AttributeValueDto.Response> response = attributeValueService.createAttributeValuesBulk(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 속성 값 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<AttributeValueDto.Response>> updateAttributeValue(
            @PathVariable Long id,
            @Valid @RequestBody AttributeValueDto.Request request) {
        log.info("PUT /api/attribute-values/{} - 속성 값 수정", id);
        
        AttributeValueDto.Response response = attributeValueService.updateAttributeValue(id, request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 값 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteAttributeValue(@PathVariable Long id) {
        log.info("DELETE /api/attribute-values/{} - 속성 값 삭제", id);
        
        attributeValueService.deleteAttributeValue(id);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 값 활성화/비활성화 토글
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ResponseDto<AttributeValueDto.Response>> toggleAttributeValue(@PathVariable Long id) {
        log.info("PATCH /api/attribute-values/{}/toggle - 속성 값 활성화 상태 변경", id);
        
        AttributeValueDto.Response response = attributeValueService.toggleAttributeValue(id);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 값 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<AttributeValueDto.Response>> getAttributeValue(@PathVariable Long id) {
        log.info("GET /api/attribute-values/{} - 속성 값 조회", id);
        
        AttributeValueDto.Response response = attributeValueService.getAttributeValue(id);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입별 속성 값 조회 (전체)
     */
    @GetMapping("/by-type/{attributeTypeId}")
    public ResponseEntity<ResponseDto<List<AttributeValueDto.Response>>> getAttributeValuesByType(
            @PathVariable Long attributeTypeId) {
        log.info("GET /api/attribute-values/by-type/{} - 속성 타입별 속성 값 조회", attributeTypeId);
        
        List<AttributeValueDto.Response> response = attributeValueService.getAttributeValuesByType(attributeTypeId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 속성 타입별 활성 속성 값만 조회
     */
    @GetMapping("/by-type/{attributeTypeId}/active")
    public ResponseEntity<ResponseDto<List<AttributeValueDto.Response>>> getActiveAttributeValuesByType(
            @PathVariable Long attributeTypeId) {
        log.info("GET /api/attribute-values/by-type/{}/active - 속성 타입별 활성 속성 값 조회", attributeTypeId);
        
        List<AttributeValueDto.Response> response = attributeValueService.getActiveAttributeValuesByType(attributeTypeId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}

