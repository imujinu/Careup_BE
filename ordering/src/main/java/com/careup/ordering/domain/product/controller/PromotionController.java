package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.PromotionRequestDto;
import com.careup.ordering.domain.product.dto.PromotionResponseDto;
import com.careup.ordering.domain.product.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로모션 관리 컨트롤러
 * 프로모션 CRUD 전용 (가격 계산은 BranchProductController에서 자동 처리)
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    
    private final PromotionService promotionService;
    
    /**
     * REQ-056: 상품 프로모션 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto> createPromotion(@RequestBody PromotionRequestDto request) {
        PromotionResponseDto response = promotionService.createPromotion(request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * REQ-057: 상품 프로모션 수정
     */
    @PutMapping("/{promotionId}")
    public ResponseEntity<ResponseDto> updatePromotion(
            @PathVariable Long promotionId,
            @RequestBody PromotionRequestDto request) {
        PromotionResponseDto response = promotionService.updatePromotion(promotionId, request);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * REQ-057: 상품 프로모션 삭제
     */
    @DeleteMapping("/{promotionId}")
    public ResponseEntity<ResponseDto> deletePromotion(@PathVariable Long promotionId) {
        promotionService.deletePromotion(promotionId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 프로모션 목록 조회
     */
    @GetMapping
    public ResponseEntity<ResponseDto> getAllPromotions() {
        List<PromotionResponseDto> response = promotionService.getAllPromotions();
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 프로모션 상세 조회
     */
    @GetMapping("/{promotionId}")
    public ResponseEntity<ResponseDto> getPromotion(@PathVariable Long promotionId) {
        PromotionResponseDto response = promotionService.getPromotion(promotionId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 지점 상품별 프로모션 조회
     */
    @GetMapping("/branch-product/{branchProductId}")
    public ResponseEntity<ResponseDto> getPromotionsByBranchProduct(
            @PathVariable Long branchProductId) {
        List<PromotionResponseDto> response = promotionService.getPromotionsByBranchProduct(branchProductId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 활성화된 프로모션 조회
     */
    @GetMapping("/active")
    public ResponseEntity<ResponseDto> getActivePromotions() {
        List<PromotionResponseDto> response = promotionService.getActivePromotions();
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}
