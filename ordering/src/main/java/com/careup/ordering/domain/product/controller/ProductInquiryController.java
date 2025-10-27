package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.ProductInquiryRequestDto;
import com.careup.ordering.domain.product.dto.ProductInquiryResponseDto;
import com.careup.ordering.domain.product.service.ProductInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 문의 컨트롤러
 * 상품 문의사항
 */
@RestController
@RequestMapping("/api/product-inquiries")
@RequiredArgsConstructor
public class ProductInquiryController {
    
    private final ProductInquiryService productInquiryService;
    
    /**
     * 상품 문의사항 작성
     */
    @PostMapping
    public ResponseEntity<ResponseDto> createInquiry(
            @RequestBody ProductInquiryRequestDto request) {
        ProductInquiryResponseDto inquiry = productInquiryService.createInquiry(request);
        return new ResponseEntity<>(ResponseDto.ok(inquiry, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     * 상품별 문의 목록 조회
     */
    @GetMapping("/branch-product/{branchProductId}")
    public ResponseEntity<ResponseDto> getInquiriesByBranchProduct(
            @PathVariable Long branchProductId) {
        List<ProductInquiryResponseDto> inquiries = 
                productInquiryService.getInquiriesByBranchProduct(branchProductId);
        return new ResponseEntity<>(ResponseDto.ok(inquiries, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 내 문의 목록 조회
     */
    @GetMapping("/my-inquiries/{memberId}")
    public ResponseEntity<ResponseDto> getMyInquiries(@PathVariable Long memberId) {
        List<ProductInquiryResponseDto> inquiries = 
                productInquiryService.getMyInquiries(memberId);
        return new ResponseEntity<>(ResponseDto.ok(inquiries, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 문의 상세 조회
     */
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto> getInquiry(@PathVariable Long inquiryId) {
        ProductInquiryResponseDto inquiry = productInquiryService.getInquiry(inquiryId);
        return new ResponseEntity<>(ResponseDto.ok(inquiry, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 문의 수정
     */
    @PutMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestBody ProductInquiryRequestDto request) {
        ProductInquiryResponseDto inquiry = 
                productInquiryService.updateInquiry(inquiryId, request);
        return new ResponseEntity<>(ResponseDto.ok(inquiry, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 문의 삭제
     */
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto> deleteInquiry(@PathVariable Long inquiryId) {
        productInquiryService.deleteInquiry(inquiryId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 답변 대기 중인 문의 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<ResponseDto> getPendingInquiries() {
        List<ProductInquiryResponseDto> inquiries = 
                productInquiryService.getPendingInquiries();
        return new ResponseEntity<>(ResponseDto.ok(inquiries, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 상품별 답변 대기 중인 문의 조회
     */
    @GetMapping("/branch-product/{branchProductId}/pending")
    public ResponseEntity<ResponseDto> getPendingInquiriesByProduct(
            @PathVariable Long branchProductId) {
        List<ProductInquiryResponseDto> inquiries = 
                productInquiryService.getPendingInquiriesByProduct(branchProductId);
        return new ResponseEntity<>(ResponseDto.ok(inquiries, HttpStatus.OK), HttpStatus.OK);
    }
}
