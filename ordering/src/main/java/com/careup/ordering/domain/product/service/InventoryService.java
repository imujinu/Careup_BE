package com.careup.ordering.domain.product.service;

import com.careup.ordering.common.service.DistributedLockService;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.dto.PromotionPriceDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.InventoryFlowDetailRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryService {

    private final BranchProductRepository branchProductRepository;
    private final InventoryFlowDetailRepository inventoryFlowDetailRepository;
    private final ProductRepository productRepository;
    private final PromotionService promotionService;
    
    // redis와 kafka 추가
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DistributedLockService distributedLockService;
    
    private static final String INVENTORY_CACHE_PREFIX = "inventory:branch:";
    private static final String INVENTORY_CHANGE_TOPIC = "inventory-change";

    // ========== 지점 상품 조회 (프로모션 포함) ==========

    /**
     *  전체 지점 상품 조회 (프로모션 포함)
     */
    @Transactional(readOnly = true)
    public List<BranchProductResponseDto> getAllBranchProductsWithPromotion() {
        List<BranchProduct> products = branchProductRepository.findAll();
        
        return products.stream()
                .map(this::convertToDtoWithPromotion)
                .collect(Collectors.toList());
    }
    
    /**
     *  지점별 상품 조회 (프로모션 포함)
     */
    @Transactional(readOnly = true)
    public List<BranchProductResponseDto> getBranchProductsByBranchWithPromotion(Long branchId) {
        List<BranchProduct> products = branchProductRepository.findByBranchId(branchId);
        
        return products.stream()
                .map(this::convertToDtoWithPromotion)
                .collect(Collectors.toList());
    }
    
    /**
     *  지점 상품 상세 조회 (프로모션 포함)
     */
    @Transactional(readOnly = true)
    public BranchProductResponseDto getBranchProductWithPromotion(Long branchProductId) {
        BranchProduct product = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("지점 상품을 찾을 수 없습니다."));
        
        return convertToDtoWithPromotion(product);
    }
    
    /**
     *  상품명으로 검색 (프로모션 포함)
     */
    @Transactional(readOnly = true)
    public List<BranchProductResponseDto> searchBranchProductsWithPromotion(String keyword) {
        List<BranchProduct> products = branchProductRepository
                .findByProduct_NameContaining(keyword);
        
        return products.stream()
                .map(this::convertToDtoWithPromotion)
                .collect(Collectors.toList());
    }
    
    /**
     *  지점별 + 상품명 검색 (프로모션 포함)
     */
    @Transactional(readOnly = true)
    public List<BranchProductResponseDto> searchBranchProductsByBranchWithPromotion(Long branchId, String keyword) {
        List<BranchProduct> products = branchProductRepository
                .findByBranchIdAndProduct_NameContaining(branchId, keyword);
        
        return products.stream()
                .map(this::convertToDtoWithPromotion)
                .collect(Collectors.toList());
    }
    
    /**
     * BranchProduct → DTO 변환 (프로모션 정보 포함)
     */
    private BranchProductResponseDto convertToDtoWithPromotion(BranchProduct branchProduct) {
        try {
            // 프로모션 가격 계산
            PromotionPriceDto promotionPrice = promotionService.calculatePromotionPrice(
                    branchProduct.getId()
            );
            
            // 프로모션 정보 포함하여 DTO 생성
            return BranchProductResponseDto.fromWithPromotion(branchProduct, promotionPrice);
            
        } catch (Exception e) {
            log.warn("프로모션 정보 조회 실패 (branchProductId: {}): {}", 
                    branchProduct.getId(), e.getMessage());
            
            // 프로모션 조회 실패 시 기본 정보만 반환
            return BranchProductResponseDto.from(branchProduct);
        }
    }

    // ========== 기존 재고 관리 기능 ==========

    // 지점별 상품 등록
    public BranchProduct createBranchProduct(Long productId, Long branchId, String serialNumber,
                                           Long stockQuantity, Long safetyStock, Long price) {
        // Product 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        // BranchProduct 생성
        BranchProduct branchProduct = BranchProduct.builder()
                .product(product)
                .branchId(branchId)
                .serialNumber(serialNumber)
                .stockQuantity(stockQuantity != null ? stockQuantity : 0L)
                .safetystock(safetyStock != null ? safetyStock : 0L)
                .price(price)
                .build();
        
        return branchProductRepository.save(branchProduct);
    }

    // 지점별 재고 조회 (프로모션 미포함 - 재고 관리용)
    @Transactional(readOnly = true)
    public List<BranchProduct> getBranchProducts(Long branchId) {
        return branchProductRepository.findByBranchId(branchId);
    }

    // 특정 상품의 지점별 재고 조회
    @Transactional(readOnly = true)
    public BranchProduct getBranchProduct(Long branchId, Long productId) {
        return branchProductRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점의 상품을 찾을 수 없습니다"));
    }

    // 안전재고 설정
    public void updateSafetyStock(Long branchProductId, Long safetyStock) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        branchProduct.updateSafetyStock(safetyStock);
        branchProductRepository.save(branchProduct);
    }

    // 재고 증감 (분산 락 적용)
    public void adjustStock(Long branchProductId, Long quantity, String type, String reason) {
        
        // 분산 락 동시성 제어
        distributedLockService.executeInventoryLock(branchProductId, () -> {
            return performStockAdjustment(branchProductId, quantity, type, reason);
        });
    }
    
    // 실제 재고 증감 로직
    private Void performStockAdjustment(Long branchProductId, Long quantity, String type, String reason) {
        
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        Long oldStock = branchProduct.getStockQuantity();
        
        if ("INCREASE".equals(type)) {
            branchProduct.increaseStock(quantity);
        } else if ("DECREASE".equals(type)) {
            // 재고 부족 확인
            if (branchProduct.getStockQuantity() < quantity) {
                log.warn("재고 부족: 현재재고={}, 요청수량={}, 부족수량={}",
                    branchProduct.getStockQuantity(), quantity, 
                    quantity - branchProduct.getStockQuantity());
                
                // 재고 부족 시 에러 발생
                throw new IllegalArgumentException(
                    String.format("재고가 부족합니다. 현재 재고: %d개, 요청 수량: %d개", 
                        branchProduct.getStockQuantity(), quantity));
                
                // 부분주문이 된다면
                // Long availableStock = branchProduct.getStockQuantity();
                // branchProduct.decreaseStock(availableStock);

            }
            branchProduct.decreaseStock(quantity);
        }
        
        branchProductRepository.save(branchProduct);

        // redis 캐시 업데이트
        updateInventoryCache(branchProduct);
        
        // 재고 변경 이벤트 발송
        publishInventoryChangeEvent(branchProduct, quantity, type, reason);
        
        // 입출고 기록 생성
        InventoryFlowDetail flow = InventoryFlowDetail.builder()
                .branchProduct(branchProduct)
                .inQuantity("INCREASE".equals(type) ? quantity : null)
                .outQuantity("DECREASE".equals(type) ? quantity : null)
                .remark(reason)
                .build();
        
        inventoryFlowDetailRepository.save(flow);
        
        return null;
    }

    // 입출고 기록 등록
    public InventoryFlowDetail createInventoryFlow(Long branchProductId, Long inQuantity, 
                                                  Long outQuantity, String remark) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        InventoryFlowDetail flow = InventoryFlowDetail.builder()
                .branchProduct(branchProduct)
                .inQuantity(inQuantity)
                .outQuantity(outQuantity)
                .remark(remark)
                .build();
        
        InventoryFlowDetail savedFlow = inventoryFlowDetailRepository.save(flow);
        
        // 재고 업데이트
        if (inQuantity != null) {
            branchProduct.increaseStock(inQuantity);
        }
        if (outQuantity != null) {
            branchProduct.decreaseStock(outQuantity);
        }
        branchProductRepository.save(branchProduct);
        
        return savedFlow;
    }

    // 입출고 기록 조회
    @Transactional(readOnly = true)
    public List<InventoryFlowDetail> getInventoryFlows(Long branchId, Long productId) {
        if (branchId != null && productId != null) {
            return inventoryFlowDetailRepository.findByBranchIdAndProductId(branchId, productId);
        } else if (branchId != null) {
            return inventoryFlowDetailRepository.findByBranchId(branchId);
        } else {
            return inventoryFlowDetailRepository.findAll();
        }
    }

    // 입출고 기록 삭제
    public void deleteInventoryFlow(Long flowId) {
        InventoryFlowDetail flow = inventoryFlowDetailRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("입출고 기록을 찾을 수 없습니다: " + flowId));
        
        inventoryFlowDetailRepository.delete(flow);
    }

    // 재고 조절 내역 조회 (불량/폐기)
    @Transactional(readOnly = true)
    public List<InventoryFlowDetail> getAdjustmentHistory(Long branchId, String reason) {
        if (branchId != null && reason != null) {
            return inventoryFlowDetailRepository.findByBranchIdAndRemarkContaining(branchId, reason);
        } else if (branchId != null) {
            return inventoryFlowDetailRepository.findByBranchId(branchId);
        } else if (reason != null) {
            return inventoryFlowDetailRepository.findByRemarkContaining(reason);
        } else {
            return inventoryFlowDetailRepository.findAll();
        }
    }
    
    // redis/kafka
    // redis 캐시 업데이트
    private void updateInventoryCache(BranchProduct branchProduct) {
        try {
            String cacheKey = INVENTORY_CACHE_PREFIX + branchProduct.getBranchId() + ":product:" + branchProduct.getProduct().getId();
            log.info("🔥 Redis 캐시 키: {}", cacheKey);
            
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("branchProductId", branchProduct.getId());
            cacheData.put("branchId", branchProduct.getBranchId());
            cacheData.put("productId", branchProduct.getProduct().getId());
            cacheData.put("stockQuantity", branchProduct.getStockQuantity());
            cacheData.put("safetyStock", branchProduct.getSafetystock());
            cacheData.put("price", branchProduct.getPrice());
            
            redisTemplate.opsForValue().set(cacheKey, cacheData, Duration.ofMinutes(30));
            log.info("재고 캐시 업데이트 완료: branchId={}, productId={}", branchProduct.getBranchId(), branchProduct.getProduct().getId());
        } catch (Exception e) {
            log.error("재고 캐시 업데이트 실패: branchProductId={}", branchProduct.getId(), e);
        }
    }
    
    // 재고 변경 이벤트 발송
    private void publishInventoryChangeEvent(BranchProduct branchProduct, Long quantity, String type, String reason) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("branchId", branchProduct.getBranchId());
            eventData.put("productId", branchProduct.getProduct().getId());
            eventData.put("productName", branchProduct.getProduct().getName());
            eventData.put("quantity", quantity);
            eventData.put("changeType", type);
            eventData.put("reason", reason);
            eventData.put("currentStock", branchProduct.getStockQuantity());
            
            kafkaTemplate.send(INVENTORY_CHANGE_TOPIC, eventData);
            log.info("재고 변경 이벤트 발송: branchId={}, productId={}, type={}, quantity={}", 
                branchProduct.getBranchId(), branchProduct.getProduct().getId(), type, quantity);
        } catch (Exception e) {
            log.error("재고 변경 이벤트 발송 실패: branchProductId={}", branchProduct.getId(), e);
        }
    }
}
