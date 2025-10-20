package com.careup.ordering.domain.product.service;

import com.careup.ordering.common.file.AwsS3Uploader;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttribute;
import com.careup.ordering.domain.product.entity.Visibility;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AwsS3Uploader awsS3Uploader;

    /**
     * 상품 등록 (이미지 포함)
     */
    public ProductResponseDto createProduct(ProductRequestDto request, MultipartFile imageFile) {
        // 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));

        Visibility visibilityEnum = Visibility.ALL;
        if (request.getVisibility() != null) {
            try {
                visibilityEnum = Visibility.valueOf(request.getVisibility().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 visibility 값: {}, 기본값(ALL) 사용", request.getVisibility());
            }
        }

        // ⭐ 이미지 업로드 (있으면)
        String imageUrl = request.getImageUrl(); // 기존 URL 우선
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = awsS3Uploader.uploadFile("products", 0L, imageFile);
            log.info("상품 이미지 업로드 완료 - URL: {}", imageUrl);
        }

        // 상품 생성
        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .supplyPrice(request.getSupplyPrice())
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .imageUrl(imageUrl != null ? imageUrl : "default-image.jpg") // 기본 이미지
                .visibility(visibilityEnum)
                .build();

        // 속성이 있으면 함께 저장
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            request.getAttributes().forEach(attrDto -> {
                ProductAttribute attribute = ProductAttribute.builder()
                        .product(product)
                        .attributeName(attrDto.getAttributeName())
                        .attributeValue(attrDto.getAttributeValue())
                        .build();
                product.getAttributes().add(attribute);
            });
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    /**
     * 상품 수정 (이미지 포함)
     */
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto request, MultipartFile imageFile) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 새 이미지 업로드 (있으면)
        String newImageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 이미지 삭제 (default 이미지가 아니면)
            if (product.getImageUrl() != null && !product.getImageUrl().equals("default-image.jpg")) {
                try {
                    awsS3Uploader.deleteByUrl(product.getImageUrl());
                    log.info("기존 상품 이미지 삭제 완료 - URL: {}", product.getImageUrl());
                } catch (Exception e) {
                    log.warn("기존 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
                }
            }
            
            // 새 이미지 업로드
            newImageUrl = awsS3Uploader.uploadFile("products", productId, imageFile);
            log.info("새 상품 이미지 업로드 완료 - URL: {}", newImageUrl);
        }

        // 상품 정보 업데이트
        product.updateInfo(
                request.getName(),
                request.getDescription(),
                request.getSupplyPrice(),
                request.getMinPrice(),
                request.getMaxPrice(),
                newImageUrl != null ? newImageUrl : request.getImageUrl() // 새 이미지 우선
        );

        // 속성 업데이트 (있으면)
        if (request.getAttributes() != null) {
            // 기존 속성 전부 삭제
            product.getAttributes().clear();
            
            // 새로운 속성 추가
            request.getAttributes().forEach(attrDto -> {
                ProductAttribute attribute = ProductAttribute.builder()
                        .product(product)
                        .attributeName(attrDto.getAttributeName())
                        .attributeValue(attrDto.getAttributeValue())
                        .build();
                product.getAttributes().add(attribute);
            });
        }

        Product updatedProduct = productRepository.save(product);
        return ProductResponseDto.from(updatedProduct);
    }

    // ========== 조회 메서드 (기존 유지) ==========
    
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        return ProductResponseDto.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchProducts(String keyword) {
        List<Product> products = productRepository.searchByKeyword(keyword);
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchProductsByCategoryAndKeyword(Long categoryId, String keyword) {
        List<Product> products = productRepository.searchByCategoryAndKeyword(categoryId, keyword);
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        // ⭐ 이미지도 삭제
        if (product.getImageUrl() != null && !product.getImageUrl().equals("default-image.jpg")) {
            try {
                awsS3Uploader.deleteByUrl(product.getImageUrl());
                log.info("상품 이미지 삭제 완료 - URL: {}", product.getImageUrl());
            } catch (Exception e) {
                log.warn("이미지 삭제 실패 (상품은 삭제됨): {}", e.getMessage());
            }
        }
        
        product.delete();
        productRepository.save(product);
    }
}
