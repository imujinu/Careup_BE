package com.careup.ordering.domain.product.service;

import com.careup.ordering.common.client.BranchClient;
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
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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
    private final BranchClient branchClient;


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
        Page<Product> productsPage = productRepository.findByCategoryId(categoryId, Pageable.unpaged());
        List<Product> products = productsPage.getContent();
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
     * 고객용 상품 목록 조회 (페이지네이션)
     *  visibility=ALL인 활성 상품만 반환
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getPublicProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByVisibilityAndActive(Visibility.ALL, pageable);
        return products.map(ProductResponseDto::from);
    }

    /**
     * 고객용 상품 목록 조회 (판매 지점 정보 포함) - 페이지네이션 + 카테고리 필터
     *  visibility=ALL인 활성 상품만 반환
     */
    @Transactional(readOnly = true)
    public Page<ProductWithBranchesDto> getPublicProductsWithBranches(Long categoryId, Pageable pageable) {
        // 카테고리 필터링 여부에 따라 다른 쿼리 사용
        Page<Product> productsPage;
        
        if (categoryId != null) {
            // 카테고리 + visibility=ALL + 활성 필터링
            productsPage = productRepository.findByCategoryIdAndVisibilityAndActive(categoryId, Visibility.ALL, pageable);
        } else {
            // 전체 상품 중 visibility=ALL + 활성 필터링
            productsPage = productRepository.findByVisibilityAndActive(Visibility.ALL, pageable);
        }
        
        List<Product> products = productsPage.getContent();

        // 모든 상품의 지점 ID 수집
        Set<Long> allBranchIds = new HashSet<>();
        products.forEach(product -> {
            List<BranchProduct> branchProducts = branchProductRepository.findByProduct(product);
            branchProducts.stream()
                    .filter(bp -> bp.getStockQuantity() > 0)
                    .forEach(bp -> allBranchIds.add(bp.getBranchId()));
        });

        // BranchClient로 지점 정보 일괄 조회 (MSA 구조: Feign Client 사용)
        Map<Long, String> branchIdToNameMap = new HashMap<>();
        if (!allBranchIds.isEmpty()) {
            try {
                log.debug("지점 정보 조회 요청 - branchIds: {}", allBranchIds);
                Map<String, Object> branchResponse = branchClient.getBranchesByIds(new ArrayList<>(allBranchIds));
                log.debug("지점 정보 응답: {}", branchResponse);
                
                // 응답 구조: CommonSuccessDto { "result": [BranchSimpleDto { "id": 1, "name": "강남점", ... }], ... }
                Object resultObj = branchResponse.get("result");
                
                if (resultObj != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> branchesData = (List<Map<String, Object>>) resultObj;
                    
                    if (branchesData != null && !branchesData.isEmpty()) {
                        branchesData.forEach(branch -> {
                            try {
                                Object idObj = branch.get("id");
                                Object nameObj = branch.get("name");
                                
                                if (idObj != null && nameObj != null) {
                                    Long id = idObj instanceof Number 
                                        ? ((Number) idObj).longValue() 
                                        : Long.parseLong(idObj.toString());
                                    String name = nameObj.toString();
                                    
                                    if (name != null && !name.isEmpty()) {
                                        branchIdToNameMap.put(id, name);
                                        log.debug("지점 정보 매핑 - id: {}, name: {}", id, name);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("지점 데이터 파싱 중 오류 - branch: {}, error: {}", branch, e.getMessage());
                            }
                        });
                    }
                }
                
                log.info("지점 정보 조회 완료 - 조회된 지점 수: {}/{}", branchIdToNameMap.size(), allBranchIds.size());
                
                // branch-service 연결 실패 시 경고
                if (branchIdToNameMap.isEmpty() && !allBranchIds.isEmpty()) {
                    log.warn("⚠️ branch-service 연결 실패 또는 응답 없음. branch-service(8081)가 실행 중인지 확인하세요.");
                }
            } catch (Exception e) {
                log.error("지점 정보 조회 실패 (Feign Client) - branchIds: {}, error: {}", allBranchIds, e.getMessage(), e);
                log.warn("⚠️ branch-service 연결 실패. 지점명 대신 '지점 {id}' 형태로 표시됩니다.");
            }
        }

        // 최종 결과 생성
        final Map<Long, String> branchNameMap = branchIdToNameMap; // final 변수로 사용하기 위해
        List<ProductWithBranchesDto> result = products.stream()
                .map(product -> {
                    // 해당 상품을 판매하는 모든 지점 정보 조회
                    List<BranchProduct> branchProducts = branchProductRepository.findByProduct(product);

                    List<ProductWithBranchesDto.BranchInfoDto> branchInfos = branchProducts.stream()
                            .filter(bp -> bp.getStockQuantity() > 0)  // 재고 있는 지점만
                            .map(bp -> ProductWithBranchesDto.BranchInfoDto.builder()
                                    .branchId(bp.getBranchId())
                                    .branchProductId(bp.getId())  // 추가
                                    .branchName(branchNameMap.getOrDefault(bp.getBranchId(), "지점 " + bp.getBranchId()))  // 실제 지점명 사용
                                    .stockQuantity(bp.getStockQuantity())
                                    .price(bp.getPrice())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductWithBranchesDto.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .description(product.getDescription())
                            .imageUrl(product.getImageUrl())
                            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                            .minPrice(product.getMinPrice())
                            .maxPrice(product.getMaxPrice())
                            .availableBranchCount(branchInfos.size())
                            .availableBranches(branchInfos)
                            .build();
                })
                .filter(dto -> dto.getAvailableBranchCount() > 0)  // 판매 지점 있는 상품만
                .collect(Collectors.toList());

        // Page 객체로 변환
        return new PageImpl<>(result, pageable, productsPage.getTotalElements());
    }

    /**
     * 고객용 상품 검색 (판매 지점 정보 포함) - 페이지네이션 + 카테고리 필터
     *  visibility=ALL인 활성 상품만 검색
     */
    @Transactional(readOnly = true)
    public Page<ProductWithBranchesDto> searchPublicProductsWithBranches(String keyword, Long categoryId, Pageable pageable) {
        // 카테고리 + 키워드 검색
        Page<Product> productsPage;
        
        if (categoryId != null) {
            // 카테고리 + 키워드 검색
            productsPage = productRepository.searchByCategoryAndKeyword(categoryId, keyword, pageable);
        } else {
            // 전체 카테고리에서 키워드 검색
            productsPage = productRepository.searchByKeyword(keyword, pageable);
        }
        
        List<Product> products = productsPage.getContent();

        // 모든 상품의 지점 ID 수집
        Set<Long> allBranchIds = new HashSet<>();
        products.forEach(product -> {
            if (product.getVisibility() == Visibility.ALL && product.isActive()) {
                List<BranchProduct> branchProducts = branchProductRepository.findByProduct(product);
                branchProducts.stream()
                        .filter(bp -> bp.getStockQuantity() > 0)
                        .forEach(bp -> allBranchIds.add(bp.getBranchId()));
            }
        });

        // BranchClient로 지점 정보 일괄 조회
        Map<Long, String> branchIdToNameMap = new HashMap<>();
        if (!allBranchIds.isEmpty()) {
            try {
                Map<String, Object> branchResponse = branchClient.getBranchesByIds(new ArrayList<>(allBranchIds));
                // 응답 구조에 따라 파싱
                Object data = branchResponse.get("data");
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> branches = (List<Map<String, Object>>) data;
                    branches.forEach(branch -> {
                        Long id = ((Number) branch.get("branchId")).longValue();
                        String name = (String) branch.get("branchName");
                        branchIdToNameMap.put(id, name);
                    });
                } else if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> branchMap = (Map<String, Object>) data;
                    branchMap.forEach((key, value) -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> branch = (Map<String, Object>) value;
                        Long id = ((Number) branch.get("branchId")).longValue();
                        String name = (String) branch.get("branchName");
                        branchIdToNameMap.put(id, name);
                    });
                }
            } catch (Exception e) {
                log.warn("지점 정보 조회 실패: {}", e.getMessage());
            }
        }

        // 최종 결과 생성
        final Map<Long, String> branchNameMap = branchIdToNameMap;
        List<ProductWithBranchesDto> result = products.stream()
                .filter(product -> product.getVisibility() == Visibility.ALL && product.isActive())
                .map(product -> {
                    // 해당 상품을 판매하는 모든 지점 정보 조회
                    List<BranchProduct> branchProducts = branchProductRepository.findByProduct(product);

                    List<ProductWithBranchesDto.BranchInfoDto> branchInfos = branchProducts.stream()
                            .filter(bp -> bp.getStockQuantity() > 0)  // 재고 있는 지점만
                            .map(bp -> ProductWithBranchesDto.BranchInfoDto.builder()
                                    .branchId(bp.getBranchId())
                                    .branchProductId(bp.getId())  // ✅ 추가
                                    .branchName(branchNameMap.getOrDefault(bp.getBranchId(), "지점 " + bp.getBranchId()))  // ✅ 실제 지점명 사용
                                    .stockQuantity(bp.getStockQuantity())
                                    .price(bp.getPrice())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductWithBranchesDto.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .description(product.getDescription())
                            .imageUrl(product.getImageUrl())
                            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                            .minPrice(product.getMinPrice())
                            .maxPrice(product.getMaxPrice())
                            .availableBranchCount(branchInfos.size())
                            .availableBranches(branchInfos)
                            .build();
                })
                .filter(dto -> dto.getAvailableBranchCount() > 0)  // 판매 지점 있는 상품만
                .collect(Collectors.toList());

        // Page 객체로 변환
        return new PageImpl<>(result, pageable, result.size());
    }
}