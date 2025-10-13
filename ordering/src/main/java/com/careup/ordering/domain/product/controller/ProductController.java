package com.careup.ordering.domain.product.controller;

import com.careup.ordering.domain.product.dto.CategoryDto;
import com.careup.ordering.domain.product.dto.ProductDto;
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
    public ResponseEntity<CategoryDto.Response> createCategory(@RequestBody CategoryDto.Request request) {
        CategoryDto.Response response = categoryService.createCategory(request);
        return ResponseEntity.ok(response);
    }

    // 카테고리 목록 조회
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto.Response>> getCategories() {
        List<CategoryDto.Response> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }
    
    // 상품 관리

    // 상품 목록 조회
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto.Response>> getProducts() {
        List<ProductDto.Response> response = productService.getAllProducts();
        return ResponseEntity.ok(response);
    }

    // 상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDto.Response> getProduct(@PathVariable Long productId) {
        ProductDto.Response response = productService.getProduct(productId);
        return ResponseEntity.ok(response);
    }

     // 상품 등록
    @PostMapping("/products")
    public ResponseEntity<ProductDto.Response> createProduct(@RequestBody ProductDto.Request request) {
        ProductDto.Response response = productService.createProduct(request);
        return ResponseEntity.ok(response);
    }

     // 상품 수정
    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductDto.Response> updateProduct(
            @PathVariable Long productId,
            @RequestBody ProductDto.Request request) {
        ProductDto.Response response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(response);
    }

     // 상품 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }
    
}