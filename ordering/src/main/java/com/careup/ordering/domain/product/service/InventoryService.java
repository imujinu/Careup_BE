package com.careup.ordering.domain.product.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.common.service.DistributedLockService;
import com.careup.ordering.domain.notification.NotificationService;
import com.careup.ordering.domain.notification.SseNotificationResDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import io.jsonwebtoken.Claims;
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
    private static final String EMPLOYEE_BRANCH_CACHE_PREFIX = "employee:branch:";
    private static final String INVENTORY_CHANGE_TOPIC = "inventory-change";
    private final NotificationService notificationService;
    private final BranchClient branchClient;

    // 권한분리
    
    // 본사 관리자인지 확인
    private boolean isHqAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_HQ_ADMIN"));
    }
    
    // 가맹점 관리자인지 확인 (BRANCH_ADMIN, FRANCHISE_OWNER)
    private boolean isBranchAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> 
                    authority.getAuthority().equals("ROLE_BRANCH_ADMIN") ||
                    authority.getAuthority().equals("ROLE_FRANCHISE_OWNER"));
    }

    // 지점 직원인지 확인
    private boolean isBranchStaff(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF"));
    }

    // 지점 사용자(관리자 + 직원)인지 확인
    private boolean isBranchUser(Authentication auth) {
        return isBranchAdmin(auth) || isBranchStaff(auth);
    }
    
    /**
     * employeeId로 현재 활성화된 branchId 조회
     */
    private Long getBranchIdByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        
        String cacheKey = EMPLOYEE_BRANCH_CACHE_PREFIX + employeeId;
        
        try {
            // 1. Redis 캐시에서 조회
            Object cachedBranchId = redisTemplate.opsForValue().get(cacheKey);
            if (cachedBranchId != null) {
                Long branchId = cachedBranchId instanceof Number 
                    ? ((Number) cachedBranchId).longValue() 
                    : Long.valueOf(cachedBranchId.toString());
                log.debug("Redis 캐시에서 사용자 지점 정보 조회 성공. employeeId: {}, branchId: {}", employeeId, branchId);
                return branchId;
            }
            
            // 2. 캐시 미스 시 Branch 서버 API 호출
            log.debug("Redis 캐시 미스. Branch 서버에서 사용자 지점 정보 조회 시도. employeeId: {}", employeeId);
            Map<String, Object> response = branchClient.getBranchIdByEmployeeId(employeeId);
            
            if (response != null && response.containsKey("branchId")) {
                Object branchIdObj = response.get("branchId");
                if (branchIdObj != null) {
                    Long branchId = branchIdObj instanceof Number 
                        ? ((Number) branchIdObj).longValue() 
                        : Long.valueOf(branchIdObj.toString());
                    
                    // 3. 조회 결과를 캐시에 저장 (TTL: 1시간)
                    redisTemplate.opsForValue().set(cacheKey, branchId, Duration.ofHours(1));
                    
                    log.debug("Branch 서버에서 사용자 지점 정보 조회 성공 및 캐시 저장. employeeId: {}, branchId: {}", employeeId, branchId);
                    return branchId;
                }
            }
            
            log.warn("Branch 서버에서 사용자 지점 정보를 찾을 수 없습니다. employeeId: {}", employeeId);
            return null;
            
        } catch (Exception e) {
            log.error("사용자 지점 정보 조회 실패. employeeId: {}", employeeId, e);
            return null;
        }
    }
    
    /**
     * JWT에서 employeeId 추출
     */
    private Long getEmployeeIdFromAuth(Authentication auth) {
        try {
            if (auth.getDetails() instanceof Claims) {
                Claims claims = (Claims) auth.getDetails();
                Object employeeIdObj = claims.get("employeeId");
                if (employeeIdObj != null) {
                    return Long.valueOf(employeeIdObj.toString());
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
        
        try {
            if (auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object employeeIdObj = details.get("employeeId");
                if (employeeIdObj != null) {
                    return Long.valueOf(employeeIdObj.toString());
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 현재 사용자의 지점 ID 조회 (가맹점 관리자용)
     * JWT에서 branchId 추출하거나 role에 따라 결정
     */
    public Long getCurrentUserBranchId(Authentication auth) {
        // 1. JWT Claims에서 branchId 추출 시도
        try {
            if (auth.getDetails() instanceof Claims) {
                Claims claims = (Claims) auth.getDetails();
                Object branchIdObj = claims.get("branchId");
                if (branchIdObj != null) {
                    Long branchId = Long.valueOf(branchIdObj.toString());
                    return branchId;
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
        
        // 2. Authentication Details에서 branchId 추출 시도 (branch 서버용)
        try {
            if (auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object branchIdObj = details.get("branchId");
                if (branchIdObj != null) {
                    Long branchId = Long.valueOf(branchIdObj.toString());
                    return branchId;
                }
            }
        } catch (Exception e) {
            log.debug("Authentication Details에서 branchId 추출 실패: {}", e.getMessage());
        }

        if (isHqAdmin(auth)) {
            return 1L;
        }
        
        if (isBranchUser(auth)) {
            // JWT에서 employeeId 추출
            Long employeeId = getEmployeeIdFromAuth(auth);
            if (employeeId != null) {
                // 캐시/API를 통해 사용자의 실제 지점 정보 조회
                Long userBranchId = getBranchIdByEmployeeId(employeeId);
                if (userBranchId != null) {
                    return userBranchId;
                }
                log.warn("사용자의 지점 정보를 찾을 수 없습니다. employeeId: {}", employeeId);
            } else {
                log.warn("JWT에서 employeeId를 찾을 수 없습니다.");
            }
            throw new AccessDeniedException("사용자의 지점 정보를 확인할 수 없습니다.");
        }
        
        throw new AccessDeniedException("유효하지 않은 사용자 권한입니다.");
    }
    
    /**
     * 지점 접근 권한 검증
     * - 본사: 모든 지점 접근 가능
     * - 가맹점: 자신의 지점만 접근 가능
     */
    private void validateBranchAccess(Authentication auth, Long branchId) {
        log.debug("지점 접근 권한 검증 시작: branchId={}, authorities={}", 
            branchId, auth.getAuthorities());
        
        if (isHqAdmin(auth)) {
            // 본사는 모든 지점 접근 가능
            return;
        }
        
        if (isBranchUser(auth)) {
            // 가맹점은 자신의 지점만 접근 가능
            if (branchId == 1L) {
                log.debug("본사 지점 조회 허용 (자동발주 등에서 사용)");
                return;
            }
            
            log.debug("지점 사용자로 인식됨, 사용자 지점 정보 조회 시도");
            try {
                Long userBranchId = getCurrentUserBranchId(auth);
                log.debug("사용자 지점 ID: {}, 요청 지점 ID: {}", userBranchId, branchId);
                
                if (!userBranchId.equals(branchId)) {
                    log.warn("지점 접근 거부: 사용자 지점={}, 요청 지점={}", userBranchId, branchId);
                    throw new AccessDeniedException("자신의 지점만 접근 가능합니다. 요청 지점: " + branchId + ", 사용자 지점: " + userBranchId);
                }
                log.debug("지점 접근 권한 확인 완료");
            } catch (AccessDeniedException e) {
                log.error("사용자 지점 정보 조회 실패: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("지점 접근 권한 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
                throw new AccessDeniedException("지점 접근 권한을 확인할 수 없습니다: " + e.getMessage());
            }
        } else {
            log.warn("재고 관리 권한 없음: authorities={}", auth.getAuthorities());
            throw new AccessDeniedException("재고 관리 권한이 없습니다.");
        }
    }
    
    /**
     * 본사 전용 기능 권한 검증
     */
    private void validateHqOnlyAccess(Authentication auth) {
        if (!isHqAdmin(auth)) {
            throw new AccessDeniedException("본사 관리자만 접근 가능합니다.");
        }
    }

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
        
        // 중복 등록 방지
        boolean exists = branchProductRepository.existsByBranchIdAndProductId(branchId, productId);
        if (exists) {
            throw new IllegalArgumentException("이미 해당 지점에 등록된 상품입니다.");
        }
        
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
    public List<BranchProduct> getBranchProducts(Long branchId, Authentication auth) {
        // 권한 체크: Authentication이 null이면 시스템 내부 호출로 간주
        if (auth != null) {
            // 본사 상품 조회 시에는 권한 체크 안함 (상품 등록할 때 가맹점이 본사 상품 목록을 볼 수 있도록)
            if (branchId != 1L) {
                validateBranchAccess(auth, branchId);
            }
        }
        
        return branchProductRepository.findByBranchIdWithProduct(branchId);
    }

    // 특정 상품의 지점별 재고 조회
    @Transactional(readOnly = true)
    public BranchProduct getBranchProduct(Long branchId, Long productId, Authentication auth) {
        if (auth != null) {
            validateBranchAccess(auth, branchId);
        }
        
        return branchProductRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점의 상품을 찾을 수 없습니다"));
    }

    // 안전재고 설정
    public void updateSafetyStock(Long branchProductId, Long safetyStock, Authentication auth) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));

        if (auth != null) {
            validateBranchAccess(auth, branchProduct.getBranchId());
        }
        
        branchProduct.updateSafetyStock(safetyStock);
        branchProductRepository.save(branchProduct);
    }

    // 재고 정보 수정 (안전재고, 단가)
    public void updateInventoryInfo(Long branchProductId, Long safetyStock, Long unitPrice, Authentication auth) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));

        if (auth != null) {
            validateBranchAccess(auth, branchProduct.getBranchId());
        }

        if (safetyStock != null) {
            branchProduct.updateSafetyStock(safetyStock);
        }

        if (unitPrice != null) {
            branchProduct.updatePrice(unitPrice);
        }
        
        branchProductRepository.save(branchProduct);
    }

    // 지점 상품 삭제
    public void deleteBranchProduct(Long branchProductId, Authentication auth) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));

        if (auth != null) {
            validateBranchAccess(auth, branchProduct.getBranchId());
        }

        // 재고가 0이 아닌 경우 삭제 방지
        if (branchProduct.getStockQuantity() > 0) {
            throw new IllegalStateException("재고가 남아있는 상품은 삭제할 수 없습니다. 먼저 재고를 정리해주세요.");
        }

        branchProductRepository.delete(branchProduct);
    }

    // 재고 증감 (분산 락 적용)
    public void adjustStock(Long branchProductId, long quantity, String type, String reason, Authentication auth) {
        // BranchProduct를 조회해서 권한 체크
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));

        if (auth != null) {
            validateBranchAccess(auth, branchProduct.getBranchId());
        }

        SseNotificationResDto dto = null;
        if("INCREASE".equals(type)){
        dto = SseNotificationResDto.stockIncreased(branchProduct.getBranchId(), branchProduct.getProduct().getName(), quantity);
        } else if("DECREASE".equals(type)){
            dto = SseNotificationResDto.stockDecreased(branchProduct.getBranchId(), branchProduct.getProduct().getName(), quantity);
        }else{
            dto = SseNotificationResDto.stockUpdated(branchProduct.getBranchId(), branchProduct.getProduct().getName(), quantity);
        }

        notificationService.publishNotification(dto);
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
                                                  Long outQuantity, String remark, Authentication auth) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));

        if (auth != null) {
            validateBranchAccess(auth, branchProduct.getBranchId());
        }
        
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
    public List<InventoryFlowDetail> getInventoryFlows(Long branchId, Long productId, Authentication auth) {
        if (auth != null) {
            if (branchId != null) {
                validateBranchAccess(auth, branchId);
            } else {
                // branchId가 null이면 본사만 전체 조회 가능
                validateHqOnlyAccess(auth);
            }
        }
        
        if (branchId != null && productId != null) {
            return inventoryFlowDetailRepository.findByBranchIdAndProductId(branchId, productId);
        } else if (branchId != null) {
            return inventoryFlowDetailRepository.findByBranchId(branchId);
        } else {
            return inventoryFlowDetailRepository.findAll();
        }
    }

    // 입출고 기록 수정
    public InventoryFlowDetail updateInventoryFlow(Long flowId, Long inQuantity, Long outQuantity, String remark, Authentication auth) {
        InventoryFlowDetail flow = inventoryFlowDetailRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("입출고 기록을 찾을 수 없습니다: " + flowId));

        if (auth != null) {
            validateBranchAccess(auth, flow.getBranchProduct().getBranchId());
        }
        
        // 기존 수량과 새 수량의 차이 계산
        Long inDiff = (inQuantity != null ? inQuantity : 0L) - (flow.getInQuantity() != null ? flow.getInQuantity() : 0L);
        Long outDiff = (outQuantity != null ? outQuantity : 0L) - (flow.getOutQuantity() != null ? flow.getOutQuantity() : 0L);
        
        // 재고 수량 업데이트
        BranchProduct branchProduct = flow.getBranchProduct();
        if (inDiff != 0) {
            branchProduct.increaseStock(inDiff);
        }
        if (outDiff != 0) {
            branchProduct.decreaseStock(outDiff);
        }
        
        // 입출고 기록 업데이트
        flow.updateFlow(inQuantity, outQuantity, remark);
        InventoryFlowDetail savedFlow = inventoryFlowDetailRepository.save(flow);
        
        // 캐시 무효화
        String cacheKey = INVENTORY_CACHE_PREFIX + branchProduct.getBranchId();
        redisTemplate.delete(cacheKey);
        
        // Kafka 이벤트 발행
        publishInventoryChangeEvent(branchProduct, inDiff - outDiff, "ADJUST", "입출고 기록 수정");
        
        return savedFlow;
    }

    // 입출고 기록 삭제
    public void deleteInventoryFlow(Long flowId, Authentication auth) {
        InventoryFlowDetail flow = inventoryFlowDetailRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("입출고 기록을 찾을 수 없습니다: " + flowId));

        if (auth != null) {
            validateBranchAccess(auth, flow.getBranchProduct().getBranchId());
        }
        
        // 삭제 전에 재고를 원래대로 되돌리기
        BranchProduct branchProduct = flow.getBranchProduct();
        
        // 입고 기록이면 출고로, 출고 기록이면 입고로 되돌리기
        if (flow.getInQuantity() != null && flow.getInQuantity() > 0) {
            // 입고 기록 삭제 시 재고 차감
            branchProduct.decreaseStock(flow.getInQuantity());
        }
        
        if (flow.getOutQuantity() != null && flow.getOutQuantity() > 0) {
            // 출고 기록 삭제 시 재고 증가
            branchProduct.increaseStock(flow.getOutQuantity());
        }
        
        branchProductRepository.save(branchProduct);
        
        // 재고 변경 이벤트 발송
        publishInventoryChangeEvent(branchProduct, 
            (flow.getInQuantity() != null ? -flow.getInQuantity() : 0) + 
            (flow.getOutQuantity() != null ? flow.getOutQuantity() : 0), 
            "ADJUST", "입출고 기록 삭제로 인한 재고 조정");
        
        inventoryFlowDetailRepository.delete(flow);
    }

    // 재고 조절 내역 조회 (불량/폐기)
    @Transactional(readOnly = true)
    public List<InventoryFlowDetail> getAdjustmentHistory(Long branchId, String reason, Authentication auth) {
        if (auth != null) {
            if (branchId != null) {
                validateBranchAccess(auth, branchId);
            } else {
                validateHqOnlyAccess(auth);
            }
        }
        
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
