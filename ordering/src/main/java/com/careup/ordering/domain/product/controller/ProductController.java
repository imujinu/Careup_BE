package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.dto.ProductWithBranchesDto;
import com.careup.ordering.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
     * 고객용 상품 목록 조회 (페이지네이션)
     *  visibility=ALL인 활성 상품만 반환
     */
    @GetMapping("/public/products")
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getPublicProducts(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("고객용 상품 목록 조회 - 페이지: {}, 사이즈: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductResponseDto> response = productService.getPublicProducts(pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 고객용 상품 목록 조회 (판매 지점 정보 포함) - 페이지네이션 + 카테고리 필터
     *  visibility=ALL인 활성 상품만 반환
     */
    @GetMapping("/public/products/with-branches")
    public ResponseEntity<ResponseDto<Page<ProductWithBranchesDto>>> getPublicProductsWithBranches(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("고객용 상품 목록 조회 (지점 정보 포함) - 카테고리: {}, 페이지: {}, 사이즈: {}", 
                categoryId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductWithBranchesDto> response = productService.getPublicProductsWithBranches(categoryId, pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 고객용 상품 검색 (판매 지점 정보 포함) - 페이지네이션 + 카테고리 필터
     *  visibility=ALL인 활성 상품만 검색
     */
    @GetMapping("/public/products/search")
    public ResponseEntity<ResponseDto<Page<ProductWithBranchesDto>>> searchPublicProductsWithBranches(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("고객용 상품 검색 - 키워드: {}, 카테고리: {}, 페이지: {}, 사이즈: {}", 
                keyword, categoryId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductWithBranchesDto> response = productService.searchPublicProductsWithBranches(keyword, categoryId, pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 등록 (이미지 포함)
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

    /**
     * 전체 상품 조회 (페이지네이션)
     */
    @GetMapping("/products")
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getProducts(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("상품 목록 조회 - 페이지: {}, 사이즈: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductResponseDto> response = productService.getAllProducts(pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(@PathVariable Long productId) {
        log.info("상품 상세 조회 - productId: {}", productId);

        ProductResponseDto response = productService.getProduct(productId);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 카테고리별 상품 조회 (페이지네이션)
     */
    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("카테고리별 상품 조회 - categoryId: {}", categoryId);

        Page<ProductResponseDto> response = productService.getProductsByCategory(categoryId, pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 검색 (페이지네이션)
     */
    @GetMapping("/products/search")
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("상품 검색 - keyword: {}", keyword);

        Page<ProductResponseDto> response = productService.searchProducts(keyword, pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 카테고리 + 검색 (페이지네이션)
     */
    @GetMapping("/products/search/category/{categoryId}")
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> searchProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam String keyword,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("카테고리별 상품 검색 - categoryId: {}, keyword: {}", categoryId, keyword);

        Page<ProductResponseDto> response = productService.searchProductsByCategoryAndKeyword(categoryId, keyword, pageable);
        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<Void>> deleteProduct(@PathVariable Long productId) {
        log.info("상품 삭제 - productId: {}", productId);

        productService.deleteProduct(productId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}