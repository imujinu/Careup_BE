package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.Visibility;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // 상품 등록
    public ProductResponseDto createProduct(ProductRequestDto request) {
        // 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));

        Visibility visibilityEnum = Visibility.ALL;
        if (request.getVisibility() != null) {
            try {
                visibilityEnum = Visibility.valueOf(request.getVisibility().toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .supplyPrice(request.getSupplyPrice())
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .imageUrl(request.getImageUrl())
                .visibility(visibilityEnum)
                .build();

        Product savedProduct = productRepository.save(product);
        return convertToProductResponse(savedProduct);
    }

    // 상품 목록 조회
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    // 상품 상세 조회
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        return convertToProductResponse(product);
    }

    // 카테고리별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductDto.Response> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    // 상품 검색
    @Transactional(readOnly = true)
    public List<ProductDto.Response> searchProducts(String keyword) {
        List<Product> products = productRepository.searchByKeyword(keyword);
        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    // 카테고리 + 검색
    @Transactional(readOnly = true)
    public List<ProductDto.Response> searchProductsByCategoryAndKeyword(Long categoryId, String keyword) {
        List<Product> products = productRepository.searchByCategoryAndKeyword(categoryId, keyword);
        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    // 상품 수정
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 상품 정보 업데이트
        product.updateInfo(
                request.getName(),
                request.getDescription(),
                request.getSupplyPrice(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getImageUrl()
        );

        Product updatedProduct = productRepository.save(product);
        return convertToProductResponse(updatedProduct);
    }

    // 상품 삭제
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        product.delete();
        productRepository.save(product);
    }

    // dto 변환 메서드
    private ProductResponseDto convertToProductResponse(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSupplyPrice(),
                product.getMinPrice(),
                product.getMaxPrice(),
                product.getImageUrl(),
                product.getStatus().toString(),
                product.getVisibility().toString()
        );
    }
}
