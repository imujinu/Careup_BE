package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.BranchProductRequestDto;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.service.BranchProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/branch-products")
@RequiredArgsConstructor
public class BranchProductController {

    private final BranchProductService branchProductService;

    /**
     * 지점 상품 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto<BranchProductResponseDto>> createBranchProduct(
            @Valid @RequestBody BranchProductRequestDto requestDto) {
        log.info("POST /api/branch-products - 지점 상품 등록 요청");

        BranchProductResponseDto response = branchProductService.createBranchProduct(requestDto);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }

    /**
     * 지점 상품 단건 조회
     */
    @GetMapping("/{branchProductId}")
    public ResponseEntity<ResponseDto<BranchProductResponseDto>> getBranchProduct(
            @PathVariable Long branchProductId) {
        log.info("GET /api/branch-products/{} - 지점 상품 조회", branchProductId);

        BranchProductResponseDto response = branchProductService.getBranchProductById(branchProductId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 지점별 상품 목록 조회
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ResponseDto<List<BranchProductResponseDto>>> getBranchProductsByBranch(
            @PathVariable Long branchId) {
        log.info("GET /api/branch-products/branch/{} - 지점별 상품 목록 조회", branchId);

        List<BranchProductResponseDto> response = branchProductService.getBranchProductsByBranch(branchId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 재고 증가
     */
    @PutMapping("/{branchProductId}/increase-stock")
    public ResponseEntity<ResponseDto<BranchProductResponseDto>> increaseStock(
            @PathVariable Long branchProductId,
            @RequestParam Long quantity) {
        log.info("PUT /api/branch-products/{}/increase-stock - 재고 증가", branchProductId);

        BranchProductResponseDto response = branchProductService.increaseStock(branchProductId, quantity);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 재고 감소
     */
    @PutMapping("/{branchProductId}/decrease-stock")
    public ResponseEntity<ResponseDto<BranchProductResponseDto>> decreaseStock(
            @PathVariable Long branchProductId,
            @RequestParam Long quantity) {
        log.info("PUT /api/branch-products/{}/decrease-stock - 재고 감소", branchProductId);

        BranchProductResponseDto response = branchProductService.decreaseStock(branchProductId, quantity);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 안전 재고 미만 상품 조회
     */
    @GetMapping("/branch/{branchId}/low-stock")
    public ResponseEntity<ResponseDto<List<BranchProductResponseDto>>> getLowStockProducts(
            @PathVariable Long branchId) {
        log.info("GET /api/branch-products/branch/{}/low-stock - 안전 재고 미만 상품 조회", branchId);

        List<BranchProductResponseDto> response = branchProductService.getLowStockProducts(branchId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }
}
