package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.purchaseOrder.dto.FranchiseAutoOrderSettingsDto;
import com.careup.branch.domain.purchaseOrder.dto.AutoOrderHistoryDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoOrderService {

    private final PurchaseOrderService purchaseOrderService;
    private final OrderingInventoryClient orderingInventoryClient;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RedisTemplate<Object, Object> redisTemplate;
    
    private static final String AUTO_ORDER_SETTINGS_PREFIX = "auto_order_settings:branch:";

    /**
     * 재고 변경 이벤트를 받아 자동 발주 처리
     * Kafka 'inventory-change' 토픽에서 이벤트 수신
     */
    @KafkaListener(topics = "inventory-change", containerFactory = "purchaseOrderKafkaListenerContainerFactory")
    public void handleInventoryChange(String message) {
        try {
            log.info("재고 변경 이벤트 수신: {}", message);
            
            // TODO: 메시지 파싱하여 지점 ID, 상품 ID 추출
            Long branchId = 1L;
            Long productId = 1L;
            
            // 해당 지점의 상품 정보 조회
            OrderingInventoryClient.BranchProductResponseDto product = 
                orderingInventoryClient.getBranchProduct(branchId, productId);
            
            if (product != null && product.stockQuantity < product.safetyStock) {
                createAutoOrder(branchId, productId, product.safetyStock - product.stockQuantity);
            }
            
        } catch (Exception e) {
            log.error("재고 변경 이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 지정 시각에 모든 지점의 재고를 체크하여 자동 발주 실행
     */
    @Scheduled(cron = "0 10 17 * * *")
    public void checkAllBranchesInventory() {
        try {
            log.info("일일 자동 발주 체크 시작");
            
            // TODO: 모든 지점 조회
            List<Long> branchIds = List.of(1L, 2L, 3L);
            
            for (Long branchId : branchIds) {
                checkBranchInventory(branchId);
            }
            
            log.info("일일 자동 발주 체크 완료");
        } catch (Exception e) {
            log.error("일일 자동 발주 체크 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 특정 지점의 재고를 체크하여 자동 발주 실행
     */
    public void checkBranchInventory(Long branchId) {
        try {
            log.info("지점 {} 재고 체크 시작", branchId);
            
            // Redis에서 자동 발주 설정 조회
            String cacheKey = AUTO_ORDER_SETTINGS_PREFIX + branchId;
            @SuppressWarnings("unchecked")
            Map<String, Object> cachedSettings = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedSettings == null) {
                log.info("지점 {}의 자동 발주 설정이 없습니다.", branchId);
                return;
            }
            
            Boolean autoOrderEnabled = (Boolean) cachedSettings.get("autoOrderEnabled");
            if (!autoOrderEnabled) {
                log.info("지점 {}의 자동 발주가 비활성화되어 있습니다.", branchId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> autoOrderProducts = (List<Map<String, Object>>) cachedSettings.get("products");
            
            // 자동 발주 설정된 상품들만 체크 (Redis에 저장된 재고 정보 사용)
            for (Map<String, Object> autoOrderProduct : autoOrderProducts) {
                Boolean productAutoOrderEnabled = (Boolean) autoOrderProduct.get("autoOrderEnabled");
                if (!productAutoOrderEnabled) {
                    continue;
                }
                
                Long productId = Long.valueOf(autoOrderProduct.get("productId").toString());
                Long safetyStock = Long.valueOf(autoOrderProduct.get("safetyStock").toString());
                Long currentStock = Long.valueOf(autoOrderProduct.get("currentStock").toString());

                if (currentStock < safetyStock) {
                    Long orderQuantity = safetyStock - currentStock;
                    createAutoOrder(branchId, productId, orderQuantity);
                    log.info("지점 {} 상품 {} 자동 발주 실행: 현재재고={}, 안전재고={}, 발주수량={}", 
                        branchId, productId, currentStock, safetyStock, orderQuantity);
                }
            }
            
        } catch (Exception e) {
            log.error("지점 {} 재고 체크 중 오류 발생: {}", branchId, e.getMessage(), e);
        }
    }

    /**
     * 자동 발주 생성
     */
    private void createAutoOrder(Long branchId, Long productId, Long quantity) {
        try {
            // 중복 발주 방지 체크
            if (hasPendingAutoOrder(branchId, productId)) {
                log.info("지점 {} 상품 {}에 대한 대기 중인 자동 발주가 있습니다.", branchId, productId);
                return;
            }

            // 자동 발주 실행
            executeAutoOrder(branchId, productId, quantity);
            
        } catch (Exception e) {
            log.error("자동 발주 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 대기 중인 자동 발주가 있는지 확인
     */
    private boolean hasPendingAutoOrder(Long branchId, Long productId) {
        try {
            // 최근 24시간 내에 해당 지점과 상품에 대한 대기 중인 발주가 있는지 확인
            LocalDateTime checkDate = LocalDateTime.now().minusHours(24);
            
            boolean hasPending = purchaseOrderRepository.existsPendingOrderForBranchAndProduct(
                branchId, productId, checkDate);
            
            if (hasPending) {
                log.info("지점 {} 상품 {}에 대한 대기 중인 발주가 있습니다. (최근 24시간 내)", branchId, productId);
            }
            
            return hasPending;
        } catch (Exception e) {
            log.error("대기 중인 자동 발주 체크 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 자동 발주 실행
     */
    private void executeAutoOrder(Long branchId, Long productId, Long quantity) {
        try {
            log.info("자동 발주 실행: 지점={}, 상품={}, 수량={}", branchId, productId, quantity);
            
            // 발주 요청 생성
            PurchaseOrderRequestDto request = PurchaseOrderRequestDto.builder()
                .branchId(branchId)
                .orderDetails(List.of(
                    PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto.builder()
                        .productId(productId)
                        .quantity(quantity.intValue())
                        .build()
                ))
                .build();

            // 자동 발주 생성 (권한 검증 없음)
            PurchaseOrderResponseDto response = purchaseOrderService.createAutoPurchaseOrder(request);
            
            log.info("자동 발주 생성 완료: {}", response);
            
        } catch (Exception e) {
            log.error("자동 발주 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 수동으로 특정 지점의 자동 발주 실행
     */
    public void executeManualAutoOrderForBranch(Long branchId) {
        try {
            log.info("수동 자동 발주 실행: 지점={}", branchId);
            checkBranchInventory(branchId);
        } catch (Exception e) {
            log.error("수동 자동 발주 실행 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 수동으로 특정 상품의 자동 발주 실행
     */
    public void executeManualAutoOrderForProduct(Long branchId, Long productId) {
        try {
            log.info("수동 자동 발주 실행: 지점={}, 상품={}", branchId, productId);
            
            // 상품 정보 조회
            OrderingInventoryClient.BranchProductResponseDto product = 
                orderingInventoryClient.getBranchProduct(branchId, productId);
            
            if (product != null && product.stockQuantity < product.safetyStock) {
                createAutoOrder(branchId, productId, product.safetyStock - product.stockQuantity);
            } else {
                log.info("자동 발주가 필요하지 않습니다. 현재 재고: {}, 안전 재고: {}", 
                    product.stockQuantity, product.safetyStock);
            }
            
        } catch (Exception e) {
            log.error("수동 자동 발주 실행 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 가맹점 자동 발주 설정 조회
     */
    public FranchiseAutoOrderSettingsDto getFranchiseAutoOrderSettings(Long branchId) {
        try {
            log.info("가맹점 {} 자동 발주 설정 조회", branchId);
            
            String cacheKey = AUTO_ORDER_SETTINGS_PREFIX + branchId;
            
            // Redis에서 기존 설정 조회
            @SuppressWarnings("unchecked")
            Map<String, Object> cachedSettings = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedSettings != null) {
                // 캐시된 설정이 있으면 반환
                log.info("Redis에서 자동 발주 설정 조회: branchId={}", branchId);
                
                Boolean autoOrderEnabled = (Boolean) cachedSettings.get("autoOrderEnabled");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> products = (List<Map<String, Object>>) cachedSettings.get("products");
                
                List<FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto> productSettings = 
                    products.stream()
                        .map(product -> FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto.builder()
                            .productId(Long.valueOf(product.get("productId").toString()))
                            .productName(product.get("productName").toString())
                            .autoOrderEnabled((Boolean) product.get("autoOrderEnabled"))
                            .safetyStock(Long.valueOf(product.get("safetyStock").toString()))
                            .currentStock(Long.valueOf(product.get("currentStock").toString()))
                            .build())
                        .collect(Collectors.toList());
                
                return FranchiseAutoOrderSettingsDto.builder()
                    .branchId(branchId)
                    .autoOrderEnabled(autoOrderEnabled)
                    .products(productSettings)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            } else {
                // 캐시된 설정이 없으면 기본 설정 반환
                log.info("Redis에 자동 발주 설정 없음: 기본 비활성 설정 반환 branchId={}", branchId);

                return FranchiseAutoOrderSettingsDto.builder()
                    .branchId(branchId)
                    .autoOrderEnabled(false) // 기본값: 비활성화
                    .products(Collections.emptyList())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            }
                
        } catch (Exception e) {
            log.error("가맹점 자동 발주 설정 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 가맹점 자동 발주 설정 업데이트
     */
    public FranchiseAutoOrderSettingsDto updateFranchiseAutoOrderSettings(Long branchId, Map<String, Object> settings) {
        try {
            log.info("가맹점 {} 자동 발주 설정 업데이트: {}", branchId, settings);
            
            Boolean autoOrderEnabled = (Boolean) settings.get("autoOrderEnabled");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) settings.get("products");
            
            // Redis에 저장할 설정 데이터 구성
            Map<String, Object> settingsToSave = new HashMap<>();
            settingsToSave.put("autoOrderEnabled", autoOrderEnabled);
            settingsToSave.put("products", products);
            settingsToSave.put("updatedAt", LocalDateTime.now().toString());
            
            // Redis에 저장 (30일 만료)
            String cacheKey = AUTO_ORDER_SETTINGS_PREFIX + branchId;
            redisTemplate.opsForValue().set(cacheKey, settingsToSave, 30, TimeUnit.DAYS);
            
            log.info("가맹점 {} 자동 발주 설정 Redis 저장 완료: 상품수={}", branchId, products.size());
            
            // 응답 DTO 생성
            List<FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto> productSettings = 
                products.stream()
                    .map(product -> {
                        Long productId = product.get("productId") != null ? 
                            Long.valueOf(product.get("productId").toString()) : 
                            Long.valueOf(product.get("id").toString());
                        
                        String productName = product.get("productName") != null ? 
                            product.get("productName").toString() : 
                            product.get("name").toString();
                        
                        Boolean productAutoOrderEnabled = product.get("autoOrderEnabled") != null ? 
                            (Boolean) product.get("autoOrderEnabled") : false;
                        
                        Long safetyStock = product.get("safetyStock") != null ? 
                            Long.valueOf(product.get("safetyStock").toString()) : 0L;
                        
                        Long currentStock = product.get("currentStock") != null ? 
                            Long.valueOf(product.get("currentStock").toString()) : 
                            Long.valueOf(product.get("stockQuantity").toString());
                        
                        return FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto.builder()
                            .productId(productId)
                            .productName(productName)
                            .autoOrderEnabled(productAutoOrderEnabled)
                            .safetyStock(safetyStock)
                            .currentStock(currentStock)
                            .build();
                    })
                    .collect(Collectors.toList());
            
            return FranchiseAutoOrderSettingsDto.builder()
                .branchId(branchId)
                .autoOrderEnabled(autoOrderEnabled)
                .products(productSettings)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("가맹점 자동 발주 설정 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 가맹점 자동 발주 히스토리 조회
     */
    public List<AutoOrderHistoryDto> getFranchiseAutoOrderHistory(Long branchId) {
        try {
            log.info("가맹점 {} 자동 발주 히스토리 조회", branchId);
            
            // TODO: 실제 자동 발주 히스토리 조회 로직 구현
            List<AutoOrderHistoryDto> history = new ArrayList<>();

            AutoOrderHistoryDto mockHistory = AutoOrderHistoryDto.builder()
                .autoOrderId(1L)
                .branchId(branchId)
                .branchName("강남점")
                .purchaseOrderId(100L)
                .reason("재고 부족으로 인한 자동 발주")
                .status("완료")
                .createdAt(LocalDateTime.now().minusHours(2))
                .products(List.of(
                    AutoOrderHistoryDto.AutoOrderProductDto.builder()
                        .productId(59L)
                        .productName("아메리카노")
                        .quantity(30L)
                        .currentStock(0L)
                        .safetyStock(30L)
                        .build()
                ))
                .build();
            
            history.add(mockHistory);
            
            return history;
            
        } catch (Exception e) {
            log.error("가맹점 자동 발주 히스토리 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
}
