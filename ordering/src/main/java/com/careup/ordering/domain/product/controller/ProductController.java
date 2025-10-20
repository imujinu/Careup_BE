package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 등록 (이미지 포함)
     * POST /api/products
     */
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ProductResponseDto>> createProduct(
            @RequestPart("product") ProductRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        
        log.info("상품 등록 요청 - 상품명: {}, 이미지: {}", 
                request.getName(), 
                imageFile != null ? imageFile.getOriginalFilename() : "없음");
        
        ProductResponseDto response = productService.createProduct(request, imageFile);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }

    /**
     * 상품 수정 (이미지 포함)
     * PUT /api/products/{productId}
     */
    @PutMapping(value = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ProductResponseDto>> updateProduct(
            @PathVariable Long productId,
            @RequestPart("product") ProductRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        
        log.info("상품 수정 요청 - productId: {}, 이미지: {}", 
                productId, 
                imageFile != null ? imageFile.getOriginalFilename() : "없음");
        
        ProductResponseDto response = productService.updateProduct(productId, request, imageFile);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    // ========== 조회 API (기존 유지) ==========
    
    @GetMapping("/products")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProducts() {
        List<ProductResponseDto> response = productService.getAllProducts();
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    // 상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(@PathVariable Long productId) {
        ProductResponseDto response = productService.getProduct(productId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    // 카테고리별 상품 조회
    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductResponseDto> response = productService.getProductsByCategory(categoryId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    // 상품 검색
    @GetMapping("/products/search")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> searchProducts(@RequestParam String keyword) {
        List<ProductResponseDto> response = productService.searchProducts(keyword);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    // 카테고리 + 검색 (옵션)
    @GetMapping("/products/search/category/{categoryId}")
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> searchProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam String keyword) {
        List<ProductResponseDto> response = productService.searchProductsByCategoryAndKeyword(categoryId, keyword);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

     // 상품 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}
