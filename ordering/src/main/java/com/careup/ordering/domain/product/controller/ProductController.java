package com.careup.ordering.domain.product.controller;

import com.careup.ordering.domain.product.dto.CategoryRequestDto;
import com.careup.ordering.domain.product.dto.CategoryResponseDto;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.service.CategoryService;
import com.careup.ordering.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    // 카테고리

    // 카테고리 등록
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponseDto> createCategory(@RequestBody CategoryRequestDto request) {
        CategoryResponseDto response = categoryService.createCategory(request);
        return ResponseEntity.ok(response);
    }

    // 카테고리 목록 조회
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getCategories() {
        List<CategoryResponseDto> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    // 상품 관리

    // 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getProducts() {
        List<ProductResponseDto> response = productService.getAllProducts();
        return ResponseEntity.ok(response);
    }

    // 상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long productId) {
        ProductResponseDto response = productService.getProduct(productId);
        return ResponseEntity.ok(response);
    }

     // 상품 등록
    @PostMapping("/products")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody ProductRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.ok(response);
    }

    // 카테고리별 상품 조회
    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<List<ProductDto.Response>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductDto.Response> response = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    // 상품 검색
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductDto.Response>> searchProducts(@RequestParam String keyword) {
        List<ProductDto.Response> response = productService.searchProducts(keyword);
        return ResponseEntity.ok(response);
    }

    // 카테고리 + 검색 (옵션)
    @GetMapping("/products/search/category/{categoryId}")
    public ResponseEntity<List<ProductDto.Response>> searchProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam String keyword) {
        List<ProductDto.Response> response = productService.searchProductsByCategoryAndKeyword(categoryId, keyword);
        return ResponseEntity.ok(response);
    }

     // 상품 수정
    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductRequestDto request) {
        ProductResponseDto response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(response);
    }

     // 상품 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

}