package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.ProductDto;
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
    public ProductDto.Response createProduct(ProductDto.Request request) {
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
    public List<ProductDto.Response> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    // 상품 상세 조회
    @Transactional(readOnly = true)
    public ProductDto.Response getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        return convertToProductResponse(product);
    }

    // 상품 수정
    public ProductDto.Response updateProduct(Long productId, ProductDto.Request request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        // 상품 정보 업데이트
        product.updateInfo(
                request.getName(),
                request.getDescription(),
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
    private ProductDto.Response convertToProductResponse(Product product) {
        return new ProductDto.Response(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getMinPrice(),
                product.getMaxPrice(),
                product.getImageUrl(),
                product.getStatus().toString(),
                product.getVisibility().toString()
        );
    }
}
