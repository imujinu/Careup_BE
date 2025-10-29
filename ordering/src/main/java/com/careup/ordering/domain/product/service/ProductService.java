package com.careup.ordering.domain.product.service;

import com.careup.ordering.common.file.AwsS3Uploader;
import com.careup.ordering.domain.product.dto.ProductRequestDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.dto.ProductWithBranchesDto;
import com.careup.ordering.domain.product.entity.*;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.Visibility;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final BranchProductRepository branchProductRepository;


    /**
     * 상품 등록 (이미지 포함)
     */
    public ProductResponseDto createProduct(ProductRequestDto request, MultipartFile imageFile) {
        // 카테고리 조회
        Category category;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));
        } else {
            throw new IllegalArgumentException("카테고리 ID가 필요합니다.");
        }

        Visibility visibilityEnum = Visibility.ALL;
        if (request.getVisibility() != null) {
            try {
                visibilityEnum = Visibility.valueOf(request.getVisibility().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 visibility 값: {}, 기본값(ALL) 사용", request.getVisibility());
            }
        }

        // 이미지 업로드 (있으면)
        String imageUrl = request.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = awsS3Uploader.uploadFile("products", 0L, imageFile);
            log.info("상품 이미지 업로드 완료 - URL: {}", imageUrl);
        } else if (imageUrl == null || imageUrl.isEmpty()) {
            // 이미지가 없으면 빈 문자열로 설정
            imageUrl = "";
        }

        // 상품 생성
        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .supplyPrice(request.getSupplyPrice())
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .imageUrl(imageUrl)
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

    // 상품 목록 조회
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 상세 조회
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        return ProductResponseDto.from(product);
    }

    // 카테고리별 상품 조회
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
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
            // 기존 이미지 삭제
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                try {
                    awsS3Uploader.deleteByUrl(product.getImageUrl());
                    log.info("기존 상품 이미지 삭제 완료 - URL: {}", product.getImageUrl());
                } catch (Exception e) {
                    log.warn("기존 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
                }
            }

            newImageUrl = awsS3Uploader.uploadFile("products", productId, imageFile);
            log.info("새 상품 이미지 업로드 완료 - URL: {}", newImageUrl);
        } else if (request.getImageUrl() != null && request.getImageUrl().isEmpty()) {
            // 이미지 파일이 없고 imageUrl이 빈 문자열이면 이미지 제거
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                try {
                    awsS3Uploader.deleteByUrl(product.getImageUrl());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            }
            newImageUrl = ""; // 빈 문자열로 설정
        }
        // 상품 정보 업데이트
        product.updateInfo(
                request.getName(),
                request.getDescription(),
                request.getSupplyPrice(),
                request.getMinPrice(),
                request.getMaxPrice(),
                newImageUrl != null ? newImageUrl : request.getImageUrl()
        );

        // 속성 업데이트
        if (request.getAttributes() != null) {
            product.getAttributes().clear();

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

    /**
     * 전체 상품 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(ProductResponseDto::from);
    }

    /**
     * 카테고리별 상품 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findActiveByCategoryId(categoryId, pageable);
        return products.map(ProductResponseDto::from);
    }

    /**
     * 상품 검색 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String keyword, Pageable pageable) {
        Page<Product> products = productRepository.searchByKeyword(keyword, pageable);
        return products.map(ProductResponseDto::from);
    }

    /**
     * 카테고리 + 검색 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProductsByCategoryAndKeyword(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products = productRepository.searchByCategoryAndKeyword(categoryId, keyword, pageable);
        return products.map(ProductResponseDto::from);
    }

    /**
     * 상품 삭제
     */
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 이미지도 삭제
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

        // 관련된 지점별 상품들도 함께 삭제
        List<BranchProduct> branchProducts = branchProductRepository.findByProductId(productId);
        if (!branchProducts.isEmpty()) {
            branchProductRepository.deleteAll(branchProducts);
        }

        // 상품 삭제
        productRepository.delete(product);
    }
    /**
     * 고객용 상품 목록 조회 (판매 지점 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<ProductWithBranchesDto> getPublicProductsWithBranches() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(product -> {
                    // 해당 상품을 판매하는 모든 지점 정보 조회
                    List<BranchProduct> branchProducts = branchProductRepository.findByProduct(product);

                    List<ProductWithBranchesDto.BranchInfoDto> branchInfos = branchProducts.stream()
                            .filter(bp -> bp.getStockQuantity() > 0)  // 재고 있는 지점만
                            .map(bp -> ProductWithBranchesDto.BranchInfoDto.builder()
                                    .branchId(bp.getBranchId())
                                    .branchName("지점명")  // Branch 서비스에서 조회 필요
                                    .stockQuantity(bp.getStockQuantity())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductWithBranchesDto.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .description(product.getDescription())
                            .imageUrl(product.getImageUrl())
                            .categoryName(product.getCategory().getName())
                            .availableBranchCount(branchInfos.size())
                            .availableBranches(branchInfos)
                            .build();
                })
                .filter(dto -> dto.getAvailableBranchCount() > 0)  // 판매 지점 있는 상품만
                .collect(Collectors.toList());
    }
}