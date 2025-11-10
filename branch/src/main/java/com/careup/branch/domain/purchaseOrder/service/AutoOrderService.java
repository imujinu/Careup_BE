package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.purchaseOrder.dto.FranchiseAutoOrderSettingsDto;
import com.careup.branch.domain.purchaseOrder.dto.AutoOrderHistoryDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ObjectMapper objectMapper;
    private final BranchRepository branchRepository;
    private final JwtTokenProvider jwtTokenProvider;
    
    private static final String AUTO_ORDER_SETTINGS_PREFIX = "auto_order_settings:branch:";

    /**
     * 재고 변경 이벤트를 받아 자동 발주 처리
     * Kafka 'inventory-change' 토픽에서 이벤트 수신
     */
    @KafkaListener(topics = "inventory-change", containerFactory = "purchaseOrderKafkaListenerContainerFactory")
    public void handleInventoryChange(String message) {
        try {
            log.info("재고 변경 이벤트 수신: {}", message);
            
            // JSON 메시지 파싱하여 지점 ID, 상품 ID 추출
            Map<String, Object> eventData = parseInventoryChangeEvent(message);
            if (eventData == null) {
                log.warn("재고 변경 이벤트 파싱 실패: {}", message);
                return;
            }
            
            Long branchId = extractLongValue(eventData, "branchId");
            Long productId = extractLongValue(eventData, "productId");
            
            if (branchId == null || productId == null) {
                log.warn("재고 변경 이벤트에서 필수 정보 누락: branchId={}, productId={}, message={}", 
                    branchId, productId, message);
                return;
            }
            
            log.debug("파싱된 재고 변경 이벤트: branchId={}, productId={}", branchId, productId);

            executeWithSystemAuthentication(() -> {
                // 해당 상품의 모든 속성 조합 조회
                List<OrderingInventoryClient.BranchProductResponseDto> branchProducts = 
                    orderingInventoryClient.getBranchProducts(branchId);
                
                // 해당 productId를 가진 BranchProduct들만 필터링
                List<OrderingInventoryClient.BranchProductResponseDto> productBranchProducts = 
                    branchProducts.stream()
                        .filter(bp -> bp.productId != null && bp.productId.equals(productId))
                        .collect(Collectors.toList());
                
                // 각 속성 조합별로 재고 확인 및 자동 발주
                for (OrderingInventoryClient.BranchProductResponseDto branchProduct : productBranchProducts) {
                    Long currentStock = branchProduct.stockQuantity != null ? branchProduct.stockQuantity : 0L;
                    Long safetyStock = branchProduct.safetyStock != null ? branchProduct.safetyStock : 0L;
                    
                    if (currentStock < safetyStock) {
                        Long orderQuantity = safetyStock - currentStock;
                        log.info("재고 부족 감지: branchId={}, productId={}, branchProductId={}, 현재재고={}, 안전재고={}, 발주수량={}", 
                            branchId, productId, branchProduct.branchProductId, currentStock, safetyStock, orderQuantity);
                        createAutoOrder(branchId, productId, branchProduct.branchProductId, orderQuantity);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("재고 변경 이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 재고 변경 이벤트 JSON 메시지 파싱
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInventoryChangeEvent(String message) {
        try {
            return objectMapper.readValue(message, Map.class);
        } catch (Exception e) {
            log.error("JSON 파싱 실패: message={}, error={}", message, e.getMessage());
            return null;
        }
    }

    private Long extractLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Long 변환 실패: key={}, value={}", key, value);
                return null;
            }
        }
        
        log.warn("지원하지 않는 타입: key={}, value={}, type={}", key, value, value.getClass());
        return null;
    }

    private Long toLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: value={}", value);
            return defaultValue;
        }
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 매일 지정 시각에 모든 지점의 재고를 체크하여 자동 발주 실행
     */
    // @Scheduled(cron = "0 0 0 * * *")

    // @Scheduled(cron = "0 50 10 * * *")


    public void checkAllBranchesInventory() {
        try {
            log.info("일일 자동 발주 체크 시작");
            
            // 모든 지점 조회
            List<Branch> branches = branchRepository.findAll();
            List<Long> branchIds = branches.stream()
                    .map(Branch::getId)
                    .collect(Collectors.toList());
            
            log.info("조회된 지점 수: {}", branchIds.size());
            
            // 시스템 인증으로 모든 지점 체크
            executeWithSystemAuthentication(() -> {
                for (Long branchId : branchIds) {
                    try {
                        checkBranchInventory(branchId);
                    } catch (Exception e) {
                        log.error("지점 {} 자동 발주 체크 중 오류 발생: {}", branchId, e.getMessage(), e);
                        // 한 지점에서 오류가 나도 다른 지점은 계속 체크
                    }
                }
            });
            
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
            
            // 지점의 모든 BranchProduct 조회 (실제 재고 정보)
            List<OrderingInventoryClient.BranchProductResponseDto> branchProducts = orderingInventoryClient.getBranchProducts(branchId);
            
            // 자동 발주 설정된 상품들만 체크
            for (Map<String, Object> autoOrderProduct : autoOrderProducts) {
                Boolean productAutoOrderEnabled = (Boolean) autoOrderProduct.get("autoOrderEnabled");
                if (!productAutoOrderEnabled) {
                    continue;
                }
                
                // branchProductId가 있으면 해당 속성 조합만 체크
                Object branchProductIdObj = autoOrderProduct.get("branchProductId");
                Long productId = Long.valueOf(autoOrderProduct.get("productId").toString());
                Long safetyStock = Long.valueOf(autoOrderProduct.get("safetyStock").toString());
                
                if (branchProductIdObj != null) {
                    // 특정 속성 조합(BranchProduct)만 체크
                    Long branchProductId = Long.valueOf(branchProductIdObj.toString());
                    OrderingInventoryClient.BranchProductResponseDto branchProduct = branchProducts.stream()
                            .filter(bp -> bp.branchProductId != null && bp.branchProductId.equals(branchProductId))
                            .findFirst()
                            .orElse(null);
                    
                    if (branchProduct != null) {
                        Long currentStock = branchProduct.stockQuantity != null ? branchProduct.stockQuantity : 0L;
                        Long branchProductSafetyStock = branchProduct.safetyStock != null ? branchProduct.safetyStock : safetyStock;
                        
                        if (currentStock < branchProductSafetyStock) {
                            Long orderQuantity = branchProductSafetyStock - currentStock;
                            createAutoOrder(branchId, productId, branchProductId, orderQuantity);
                            log.info("지점 {} 상품 {} (속성 조합 ID: {}) 자동 발주 실행: 현재재고={}, 안전재고={}, 발주수량={}", 
                                branchId, productId, branchProductId, currentStock, branchProductSafetyStock, orderQuantity);
                        }
                    } else {
                        log.warn("지점 {} 상품 {} (속성 조합 ID: {})를 찾을 수 없습니다. 자동 발주를 건너뜁니다.", 
                            branchId, productId, branchProductId);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("지점 {} 재고 체크 중 오류 발생: {}", branchId, e.getMessage(), e);
        }
    }

    /**
     * 자동 발주 생성
     */
    private void createAutoOrder(Long branchId, Long productId, Long branchProductId, Long quantity) {
        try {
            // 중복 발주 방지 체크
            if (hasPendingAutoOrder(branchId, productId)) {
                log.info("지점 {} 상품 {}에 대한 대기 중인 자동 발주가 있습니다.", branchId, productId);
                return;
            }

            // 자동 발주 실행
            executeAutoOrder(branchId, productId, branchProductId, quantity);
            
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
    private void executeAutoOrder(Long branchId, Long productId, Long branchProductId, Long quantity) {
        try {
            log.info("자동 발주 실행: 지점={}, 상품={}, 속성조합ID={}, 수량={}", branchId, productId, branchProductId, quantity);
            
            // 가맹점의 branchProductId로 attributeValueId 조회
            Long attributeValueId = null;
            try {
                OrderingInventoryClient.BranchProductResponseDto branchProduct = 
                    orderingInventoryClient.getBranchProducts(branchId).stream()
                        .filter(bp -> bp.branchProductId != null && bp.branchProductId.equals(branchProductId))
                        .findFirst()
                        .orElse(null);
                
                if (branchProduct != null && branchProduct.attributeValueId != null) {
                    attributeValueId = branchProduct.attributeValueId;
                    log.info("가맹점 속성 조합 ID: {}, attributeValueId: {}", branchProductId, attributeValueId);
                }
            } catch (Exception e) {
                log.warn("가맹점 branchProductId로 attributeValueId 조회 실패: {}", e.getMessage());
            }
            
            // 발주 요청 생성
            PurchaseOrderRequestDto request = PurchaseOrderRequestDto.builder()
                .branchId(branchId)
                .orderDetails(List.of(
                    PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto.builder()
                        .productId(productId)
                        .branchProductId(branchProductId)  // 가맹점 branchProductId 전달
                        .attributeValueId(attributeValueId)  // attributeValueId 전달
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

    private void executeWithSystemAuthentication(Runnable task) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        SecurityContext context = SecurityContextHolder.getContext();
        
        try {
            String systemToken = jwtTokenProvider.createAccessToken(
                1L,
                "HQ_ADMIN",
                1L,
                "system@careup.com"
            );

            Claims systemClaims = jwtTokenProvider.parseAccessToken(systemToken);
            
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_HQ_ADMIN"));
            var systemAuth = new UsernamePasswordAuthenticationToken(
                "1",
                systemToken,
                authorities
            );
            systemAuth.setDetails(systemClaims);

            context.setAuthentication(systemAuth);

            task.run();
            
        } finally {
            context.setAuthentication(previousAuth);
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
            
            // 해당 상품의 모든 속성 조합(BranchProduct) 조회
            List<OrderingInventoryClient.BranchProductResponseDto> branchProducts = 
                orderingInventoryClient.getBranchProducts(branchId);
            
            // 해당 productId를 가진 BranchProduct들만 필터링
            List<OrderingInventoryClient.BranchProductResponseDto> productBranchProducts = 
                branchProducts.stream()
                    .filter(bp -> bp.productId != null && bp.productId.equals(productId))
                    .collect(Collectors.toList());
            
            boolean hasAutoOrder = false;
            for (OrderingInventoryClient.BranchProductResponseDto branchProduct : productBranchProducts) {
                Long currentStock = branchProduct.stockQuantity != null ? branchProduct.stockQuantity : 0L;
                Long safetyStock = branchProduct.safetyStock != null ? branchProduct.safetyStock : 0L;
                
                if (currentStock < safetyStock) {
                    Long orderQuantity = safetyStock - currentStock;
                    createAutoOrder(branchId, productId, branchProduct.branchProductId, orderQuantity);
                    log.info("속성 조합 ID: {} 자동 발주 실행: 현재재고={}, 안전재고={}, 발주수량={}", 
                        branchProduct.branchProductId, currentStock, safetyStock, orderQuantity);
                    hasAutoOrder = true;
                }
            }
            
            if (!hasAutoOrder) {
                log.info("자동 발주가 필요하지 않습니다. 모든 속성 조합의 재고가 안전재고 이상입니다.");
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
                if (products == null) {
                    products = Collections.emptyList();
                }
                
                List<FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto> productSettings = 
                    products.stream()
                        .map(product -> {
                            Long branchProductId = null;
                            if (product.get("branchProductId") != null) {
                                branchProductId = Long.valueOf(product.get("branchProductId").toString());
                            }
                            Object productIdObj = product.get("productId") != null ? 
                                product.get("productId") : 
                                product.get("id");
                            Long productId = toLong(productIdObj, null);
                            
                            Object productNameObj = product.get("productName") != null ? 
                                product.get("productName") : 
                                product.get("name");
                            String productName = productNameObj != null ? productNameObj.toString() : "";
                            
                            Long safetyStock = toLong(product.get("safetyStock"), 0L);
                            
                            Object currentStockObj = product.get("currentStock") != null ? 
                                product.get("currentStock") : 
                                product.get("stockQuantity");
                            Long currentStock = toLong(currentStockObj, 0L);
                            
                            Boolean productAutoOrderEnabled = toBoolean(product.get("autoOrderEnabled"));
                            
                            return FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto.builder()
                                .productId(productId)
                                .branchProductId(branchProductId)
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
            if (products == null) {
                products = Collections.emptyList();
            }
            
            List<Map<String, Object>> normalizedProducts = products.stream()
                .map(product -> {
                    Map<String, Object> normalized = new HashMap<>();
                    
                    Object productIdObj = product.get("productId") != null ? 
                        product.get("productId") : 
                        product.get("id");
                    normalized.put("productId", productIdObj);
                    normalized.put("branchProductId", product.get("branchProductId"));
                    
                    Object productNameObj = product.get("productName") != null ? 
                        product.get("productName") : 
                        product.get("name");
                    normalized.put("productName", productNameObj);
                    
                    normalized.put("autoOrderEnabled", toBoolean(product.get("autoOrderEnabled")));
                    
                    Long safetyStock = toLong(product.get("safetyStock"), null);
                    if (safetyStock != null) {
                        normalized.put("safetyStock", safetyStock);
                    }
                    
                    Object currentStockObj = product.get("currentStock") != null ? 
                        product.get("currentStock") : 
                        product.get("stockQuantity");
                    Long currentStock = toLong(currentStockObj, null);
                    if (currentStock != null) {
                        normalized.put("currentStock", currentStock);
                    }
                    
                    return normalized;
                })
                .collect(Collectors.toList());
            
            // Redis에 저장할 설정 데이터 구성
            Map<String, Object> settingsToSave = new HashMap<>();
            settingsToSave.put("autoOrderEnabled", autoOrderEnabled);
            settingsToSave.put("products", normalizedProducts);
            settingsToSave.put("updatedAt", LocalDateTime.now().toString());
            
            // Redis에 저장 (30일 만료)
            String cacheKey = AUTO_ORDER_SETTINGS_PREFIX + branchId;
            redisTemplate.opsForValue().set(cacheKey, settingsToSave, 30, TimeUnit.DAYS);
            
            log.info("가맹점 {} 자동 발주 설정 Redis 저장 완료: 상품수={}", branchId, products.size());
            
            // 응답 DTO 생성
            List<FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto> productSettings = 
                normalizedProducts.stream()
                    .map(product -> {
                        Long productId = toLong(product.get("productId"), null);
                        
                        Object productNameObj = product.get("productName");
                        String productName = productNameObj != null ? productNameObj.toString() : "";
                        
                        Boolean productAutoOrderEnabled = toBoolean(product.get("autoOrderEnabled"));
                        
                        Long safetyStock = toLong(product.get("safetyStock"), 0L);
                        
                        Long currentStock = toLong(product.get("currentStock"), 0L);
                        
                        Long branchProductId = null;
                        if (product.get("branchProductId") != null) {
                            branchProductId = Long.valueOf(product.get("branchProductId").toString());
                        }
                        
                        return FranchiseAutoOrderSettingsDto.ProductAutoOrderSettingDto.builder()
                            .productId(productId)
                            .branchProductId(branchProductId)
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
