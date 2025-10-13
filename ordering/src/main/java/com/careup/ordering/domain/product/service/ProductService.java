package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductStatus;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 상품 등록
     */
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        log.info("상품 등록 시작 - name: {}, categoryId: {}", 
                requestDto.getName(), requestDto.getCategoryId());

        // 카테고리 조회
        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 카테고리입니다. ID: " + requestDto.getCategoryId()));

        // 상품 생성
        Product product = Product.builder()
                .category(category)
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .supplyPrice(requestDto.getSupplyPrice())  // 공급가 추가
                .minPrice(requestDto.getMinPrice())
                .maxPrice(requestDto.getMaxPrice())
                .imageUrl(requestDto.getImageUrl())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("상품 등록 완료 - productId: {}, supplyPrice: {}", 
                savedProduct.getId(), savedProduct.getSupplyPrice());

        return ProductResponseDto.from(savedProduct);
    }

    /**
     * 상품 단건 조회
     */
    public ProductResponseDto getProductById(Long productId) {
        log.info("상품 조회 - productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 상품입니다. ID: " + productId));

        return ProductResponseDto.from(product);
    }

    /**
     * 전체 상품 조회 (활성 상품만)
     */
    public List<ProductResponseDto> getAllProducts() {
        log.info("전체 상품 조회");

        List<Product> products = productRepository.findByStatusAndIsDelYn(
                ProductStatus.ACTIVE, "N");

        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 상품 조회
     */
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        log.info("카테고리별 상품 조회 - categoryId: {}", categoryId);

        // 카테고리 존재 확인
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. ID: " + categoryId);
        }

        List<Product> products = productRepository.findByCategoryIdAndStatusAndIsDelYn(
                categoryId, ProductStatus.ACTIVE, "N");

        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 검색 (상품명)
     */
    public List<ProductResponseDto> searchProducts(String keyword) {
        log.info("상품 검색 - keyword: {}", keyword);

        List<Product> products = productRepository.findByNameContainingAndStatusAndIsDelYn(
                keyword, ProductStatus.ACTIVE, "N");

        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("상품 삭제 - productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 상품입니다. ID: " + productId));

        product.delete();
        log.info("상품 삭제 완료 - productId: {}", productId);
    }
}
