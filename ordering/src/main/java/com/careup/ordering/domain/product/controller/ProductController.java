package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto requestDto) {
        log.info("POST /api/products - 상품 등록 요청");

        ProductResponseDto response = productService.createProduct(requestDto);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }

    /**
     * 상품 단건 조회
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(
            @PathVariable Long productId) {
        log.info("GET /api/products/{} - 상품 조회", productId);

        ProductResponseDto response = productService.getProductById(productId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 전체 상품 조회
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getAllProducts() {
        log.info("GET /api/products - 전체 상품 조회");

        List<ProductResponseDto> response = productService.getAllProducts();

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 카테고리별 상품 조회
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProductsByCategory(
            @PathVariable Long categoryId) {
        log.info("GET /api/products/category/{} - 카테고리별 상품 조회", categoryId);

        List<ProductResponseDto> response = productService.getProductsByCategory(categoryId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword) {
        log.info("GET /api/products/search?keyword={} - 상품 검색", keyword);

        List<ProductResponseDto> response = productService.searchProducts(keyword);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<Void>> deleteProduct(
            @PathVariable Long productId) {
        log.info("DELETE /api/products/{} - 상품 삭제", productId);

        productService.deleteProduct(productId);

        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}
