package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.PromotionPriceDto;
import com.careup.ordering.domain.product.dto.PromotionRequestDto;
import com.careup.ordering.domain.product.dto.PromotionResponseDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Promotion;
import com.careup.ordering.domain.product.entity.PromotionStatus;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 프로모션 관리 서비스
 * - 프로모션 CRUD
 * - 프로모션 가격 계산 (내부용)
 * - 자동 만료 스케줄러
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final BranchProductRepository branchProductRepository;

    // ========== 프로모션 CRUD (관리자용) ==========
    
    /**
     * 상품 프로모션 등록
     */
    @Transactional
    public PromotionResponseDto createPromotion(PromotionRequestDto request) {
        BranchProduct branchProduct = branchProductRepository.findById(request.getBranchProductId())
                .orElseThrow(() -> new IllegalArgumentException("해당 지점 상품을 찾을 수 없습니다."));
        
        Promotion promotion = Promotion.builder()
                .branchProduct(branchProduct)
                .discountRate(request.getDiscountRate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(PromotionStatus.ACTIVE)
                .build();
        
        Promotion saved = promotionRepository.save(promotion);
        return PromotionResponseDto.from(saved);
    }
    
    /**
     * 상품 프로모션 수정
     */
    @Transactional
    public PromotionResponseDto updatePromotion(Long promotionId, PromotionRequestDto request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 프로모션을 찾을 수 없습니다."));
        
        promotion.updatePromotion(
                request.getDiscountRate(),
                request.getStartDate(),
                request.getEndDate()
        );
        
        return PromotionResponseDto.from(promotion);
    }
    
    /**
     * 상품 프로모션 삭제
     */
    @Transactional
    public void deletePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 프로모션을 찾을 수 없습니다."));
        promotionRepository.delete(promotion);
    }
    
    /**
     * 프로모션 목록 조회
     */
    public List<PromotionResponseDto> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(PromotionResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 프로모션 상세 조회
     */
    public PromotionResponseDto getPromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 프로모션을 찾을 수 없습니다."));
        return PromotionResponseDto.from(promotion);
    }
    
    /**
     * 지점 상품별 프로모션 조회
     */
    public List<PromotionResponseDto> getPromotionsByBranchProduct(Long branchProductId) {
        return promotionRepository.findByBranchProductId(branchProductId).stream()
                .map(PromotionResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 활성화된 프로모션 조회
     */
    public List<PromotionResponseDto> getActivePromotions() {
        LocalDate today = LocalDate.now();
        
        return promotionRepository.findByStatus(PromotionStatus.ACTIVE).stream()
                .filter(p -> {
                    boolean isAfterStart = !today.isBefore(p.getStartDate());
                    boolean isBeforeEnd = !today.isAfter(p.getEndDate());
                    return isAfterStart && isBeforeEnd;
                })
                .map(PromotionResponseDto::from)
                .collect(Collectors.toList());
    }
    
    // ========== 프로모션 가격 계산 (내부용 - InventoryService에서 호출) ==========
    
    /**
     * 특정 상품의 프로모션 적용 가격 계산 (내부용)
     * InventoryService에서 상품 조회 시 사용
     */
    public PromotionPriceDto calculatePromotionPrice(Long branchProductId) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점 상품을 찾을 수 없습니다."));
        
        BigDecimal originalPrice = BigDecimal.valueOf(branchProduct.getPrice());
        Optional<Promotion> activePromotion = getActivePromotionForProduct(branchProductId);
        
        if (activePromotion.isPresent()) {
            Promotion promotion = activePromotion.get();
            BigDecimal discountRate = promotion.getDiscountRate();
            BigDecimal discountAmount = originalPrice
                    .multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            BigDecimal finalPrice = originalPrice.subtract(discountAmount);
            
            return PromotionPriceDto.builder()
                    .branchProductId(branchProductId)
                    .productName(branchProduct.getProduct().getName())
                    .originalPrice(originalPrice)
                    .discountRate(discountRate)
                    .discountAmount(discountAmount)
                    .finalPrice(finalPrice)
                    .hasPromotion(true)
                    .promotionId(promotion.getId())
                    .build();
        }
        
        // 프로모션 없음
        return PromotionPriceDto.builder()
                .branchProductId(branchProductId)
                .productName(branchProduct.getProduct().getName())
                .originalPrice(originalPrice)
                .discountRate(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(originalPrice)
                .hasPromotion(false)
                .promotionId(null)
                .build();
    }
    
    /**
     * 현재 진행 중인 프로모션 중 최대 할인율 프로모션 반환 (private)
     */
    private Optional<Promotion> getActivePromotionForProduct(Long branchProductId) {
        LocalDate today = LocalDate.now();
        
        List<Promotion> activePromotions = promotionRepository
                .findByBranchProductIdAndStatus(branchProductId, PromotionStatus.ACTIVE)
                .stream()
                .filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
                .toList();
        
        return activePromotions.stream()
                .max(Comparator.comparing(Promotion::getDiscountRate));
    }
    
    // ========== 자동 스케줄러 ==========
    
    /**
     * 매일 자정에 만료된 프로모션 비활성화
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivateExpiredPromotions() {
        LocalDate today = LocalDate.now();
        
        List<Promotion> expiredPromotions = promotionRepository
                .findByStatus(PromotionStatus.ACTIVE)
                .stream()
                .filter(p -> today.isAfter(p.getEndDate()))
                .toList();
        
        if (!expiredPromotions.isEmpty()) {
            expiredPromotions.forEach(Promotion::deactivate);
            log.info("만료된 프로모션 {}개를 비활성화했습니다.", expiredPromotions.size());
        }
    }
    
    /**
     * 매일 자정에 시작 예정인 프로모션 활성화
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void activateScheduledPromotions() {
        LocalDate today = LocalDate.now();
        
        List<Promotion> scheduledPromotions = promotionRepository
                .findByStatus(PromotionStatus.INACTIVE)
                .stream()
                .filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
                .toList();
        
        if (!scheduledPromotions.isEmpty()) {
            scheduledPromotions.forEach(Promotion::activate);
            log.info("시작 예정인 프로모션 {}개를 활성화했습니다.", scheduledPromotions.size());
        }
    }
}
