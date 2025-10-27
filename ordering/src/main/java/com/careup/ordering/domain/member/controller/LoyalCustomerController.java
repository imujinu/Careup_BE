package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.member.dto.request.LoyalCustomerRegisterRequest;
import com.careup.ordering.domain.member.dto.request.LoyalCustomerUpdateRequest;
import com.careup.ordering.domain.member.dto.response.LoyalCustomerResponseDto;
import com.careup.ordering.domain.member.entity.LoyalCustomer;
import com.careup.ordering.domain.member.entity.LoyalGrade;
import com.careup.ordering.domain.member.service.LoyalCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 단골 고객 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/loyal-customers")
@RequiredArgsConstructor
public class LoyalCustomerController {
    
    private final LoyalCustomerService loyalCustomerService;
    
    /**
     *  단골 고객 조회 (지점별)
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ResponseDto> getLoyalCustomersByBranch(@PathVariable Long branchId) {
        List<LoyalCustomerResponseDto> loyalCustomers = 
                loyalCustomerService.getLoyalCustomersByBranch(branchId);
        return new ResponseEntity<>(ResponseDto.ok(loyalCustomers, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  고객 상세 조회
     */
    @GetMapping("/{loyalCustomerId}")
    public ResponseEntity<ResponseDto> getLoyalCustomer(@PathVariable Long loyalCustomerId) {
        LoyalCustomerResponseDto loyalCustomer = 
                loyalCustomerService.getLoyalCustomer(loyalCustomerId);
        return new ResponseEntity<>(ResponseDto.ok(loyalCustomer, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  단골 고객 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto> registerLoyalCustomer(
            @RequestBody LoyalCustomerRegisterRequest request) {
        LoyalCustomerResponseDto loyalCustomer = 
                loyalCustomerService.registerLoyalCustomer(request);
        return new ResponseEntity<>(ResponseDto.ok(loyalCustomer, HttpStatus.CREATED), HttpStatus.CREATED);
    }
    
    /**
     *  단골 고객 수정
     */
    @PutMapping("/{loyalCustomerId}")
    public ResponseEntity<ResponseDto> updateLoyalCustomer(
            @PathVariable Long loyalCustomerId,
            @RequestBody LoyalCustomerUpdateRequest request) {
        LoyalCustomerResponseDto loyalCustomer = 
                loyalCustomerService.updateLoyalCustomer(loyalCustomerId, request);
        return new ResponseEntity<>(ResponseDto.ok(loyalCustomer, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  단골 고객 삭제
     */
    @DeleteMapping("/{loyalCustomerId}")
    public ResponseEntity<ResponseDto> deleteLoyalCustomer(@PathVariable Long loyalCustomerId) {
        loyalCustomerService.deleteLoyalCustomer(loyalCustomerId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     * 등급별 단골 고객 조회
     */
    @GetMapping("/branch/{branchId}/grade/{grade}")
    public ResponseEntity<ResponseDto> getLoyalCustomersByGrade(
            @PathVariable Long branchId,
            @PathVariable LoyalGrade grade) {
        List<LoyalCustomerResponseDto> loyalCustomers = 
                loyalCustomerService.getLoyalCustomersByGrade(branchId, grade);
        return new ResponseEntity<>(ResponseDto.ok(loyalCustomers, HttpStatus.OK), HttpStatus.OK);
    }
}
