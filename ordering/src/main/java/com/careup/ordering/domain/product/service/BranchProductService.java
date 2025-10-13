package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.BranchProductRequestDto;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
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
public class BranchProductService {

    private final BranchProductRepository branchProductRepository;
    private final ProductRepository productRepository;

    /**
     * 지점 상품 등록
     */
    @Transactional
    public BranchProductResponseDto createBranchProduct(BranchProductRequestDto requestDto) {
        log.info("지점 상품 등록 시작 - productId: {}, branchId: {}", 
                requestDto.getProductId(), requestDto.getBranchId());

        // 상품 조회
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 상품입니다. ID: " + requestDto.getProductId()));

        // 지점 상품 생성
        BranchProduct branchProduct = BranchProduct.builder()
                .product(product)
                .branchId(requestDto.getBranchId())
                .serialNumber(requestDto.getSerialNumber())
                .stockQuantity(requestDto.getStockQuantity())
                .safetystock(requestDto.getSafetyStock())
                .price(requestDto.getPrice())
                .build();

        BranchProduct savedBranchProduct = branchProductRepository.save(branchProduct);
        log.info("지점 상품 등록 완료 - branchProductId: {}", savedBranchProduct.getId());

        return BranchProductResponseDto.from(savedBranchProduct);
    }

    /**
     * 지점 상품 단건 조회
     */
    public BranchProductResponseDto getBranchProductById(Long branchProductId) {
        log.info("지점 상품 조회 - branchProductId: {}", branchProductId);

        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지점 상품입니다. ID: " + branchProductId));

        return BranchProductResponseDto.from(branchProduct);
    }

    /**
     * 지점별 상품 목록 조회
     */
    public List<BranchProductResponseDto> getBranchProductsByBranch(Long branchId) {
        log.info("지점별 상품 목록 조회 - branchId: {}", branchId);

        List<BranchProduct> branchProducts = branchProductRepository.findByBranchId(branchId);

        return branchProducts.stream()
                .map(BranchProductResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 재고 증가
     */
    @Transactional
    public BranchProductResponseDto increaseStock(Long branchProductId, Long quantity) {
        log.info("재고 증가 - branchProductId: {}, quantity: {}", branchProductId, quantity);

        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지점 상품입니다. ID: " + branchProductId));

        branchProduct.increaseStock(quantity);
        log.info("재고 증가 완료 - 현재 재고: {}", branchProduct.getStockQuantity());

        return BranchProductResponseDto.from(branchProduct);
    }

    /**
     * 재고 감소
     */
    @Transactional
    public BranchProductResponseDto decreaseStock(Long branchProductId, Long quantity) {
        log.info("재고 감소 - branchProductId: {}, quantity: {}", branchProductId, quantity);

        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 지점 상품입니다. ID: " + branchProductId));

        branchProduct.decreaseStock(quantity);
        log.info("재고 감소 완료 - 현재 재고: {}", branchProduct.getStockQuantity());

        return BranchProductResponseDto.from(branchProduct);
    }

    /**
     * 안전 재고 미만 상품 조회
     */
    public List<BranchProductResponseDto> getLowStockProducts(Long branchId) {
        log.info("안전 재고 미만 상품 조회 - branchId: {}", branchId);

        List<BranchProduct> branchProducts = branchProductRepository.findByBranchId(branchId);

        return branchProducts.stream()
                .filter(bp -> bp.getStockQuantity() < bp.getSafetystock())
                .map(BranchProductResponseDto::from)
                .collect(Collectors.toList());
    }
}
