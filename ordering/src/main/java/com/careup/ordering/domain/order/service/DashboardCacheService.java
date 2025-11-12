package com.careup.ordering.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 대시보드 통계 Redis 캐시 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SALES_KEY_PREFIX = "dashboard:sales:branch:";
    private static final String INVENTORY_KEY_PREFIX = "dashboard:inventory:branch:";
    private static final String ORDER_KEY_PREFIX = "dashboard:order:branch:";
    private static final long CACHE_TTL_HOURS = 24;

    // ========== 매출 현황 캐시 ==========

    /**
     * 주문 생성/확정 시 매출 통계 증가
     */
    public void incrementSales(Long branchId, Long amount, LocalDate orderDate) {
        try {
            String key = getSalesKey(branchId);
            
            log.info("📊 매출 통계 증가 시작 - branchId: {}, amount: {}, date: {}", branchId, amount, orderDate);

            // 총 매출 증가
            Long newTotalSales = redisTemplate.opsForHash().increment(key, "totalSales", amount);
            log.debug("  - totalSales 증가: {}", newTotalSales);

            // 월간 매출 증가 (현재 월)
            LocalDate today = LocalDate.now();
            if (orderDate.getYear() == today.getYear() && orderDate.getMonth() == today.getMonth()) {
                Long newMonthlySales = redisTemplate.opsForHash().increment(key, "monthlySales", amount);
                log.debug("  - monthlySales 증가: {}", newMonthlySales);
            }

            // 총 주문 수 증가
            Long newTotalOrders = redisTemplate.opsForHash().increment(key, "totalOrders", 1);
            log.debug("  - totalOrders 증가: {}", newTotalOrders);

            // 일별 매출 증가 (최근 7일)
            String dailyKey = "daily:" + orderDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Long newDailySales = redisTemplate.opsForHash().increment(key, dailyKey, amount);
            log.debug("  - {} 증가: {}", dailyKey, newDailySales);

            // TTL 설정
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.info("✅ 매출 통계 증가 완료 - branchId: {}, totalSales: {}, totalOrders: {}", 
                    branchId, newTotalSales, newTotalOrders);
        } catch (Exception e) {
            log.error("❌ 매출 통계 캐시 업데이트 실패 - branchId: {}", branchId, e);
        }
    }

    /**
     * 주문 취소 시 매출 통계 감소
     */
    public void decrementSales(Long branchId, Long amount, LocalDate orderDate) {
        try {
            String key = getSalesKey(branchId);

            // 총 매출 감소
            redisTemplate.opsForHash().increment(key, "totalSales", -amount);

            // 월간 매출 감소 (현재 월)
            LocalDate today = LocalDate.now();
            if (orderDate.getYear() == today.getYear() && orderDate.getMonth() == today.getMonth()) {
                redisTemplate.opsForHash().increment(key, "monthlySales", -amount);
            }

            // 총 주문 수 감소
            redisTemplate.opsForHash().increment(key, "totalOrders", -1);

            // 일별 매출 감소 (최근 7일)
            String dailyKey = "daily:" + orderDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            redisTemplate.opsForHash().increment(key, dailyKey, -amount);

            log.debug("매출 통계 감소 - branchId: {}, amount: {}, date: {}", branchId, amount, orderDate);
        } catch (Exception e) {
            log.error("매출 통계 캐시 업데이트 실패 - branchId: {}", branchId, e);
        }
    }

    /**
     * 캐시된 매출 데이터 조회
     */
    public Map<String, Object> getCachedSalesData(Long branchId) {
        try {
            String key = getSalesKey(branchId);
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (data.isEmpty()) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            data.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception e) {
            log.error("매출 캐시 조회 실패 - branchId: {}", branchId, e);
            return null;
        }
    }

    // ========== 주문 현황 캐시 ==========

    /**
     * 주문 상태별 카운트 증가
     */
    public void incrementOrderStatus(Long branchId, String status) {
        try {
            String key = getOrderKey(branchId);

            // 총 주문 수 증가
            redisTemplate.opsForHash().increment(key, "totalOrders", 1);

            // 상태별 주문 수 증가
            String statusKey = "status:" + status;
            redisTemplate.opsForHash().increment(key, statusKey, 1);

            // TTL 설정
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.debug("주문 상태 통계 증가 - branchId: {}, status: {}", branchId, status);
        } catch (Exception e) {
            log.error("주문 상태 캐시 업데이트 실패 - branchId: {}", branchId, e);
        }
    }

    /**
     * 주문 상태 변경 시 카운트 조정
     */
    public void updateOrderStatus(Long branchId, String previousStatus, String newStatus) {
        try {
            String key = getOrderKey(branchId);

            log.info("📊 주문 상태 변경 캐시 업데이트 - branchId: {}, {} -> {}", branchId, previousStatus, newStatus);

            // 이전 상태 감소
            if (previousStatus != null) {
                String prevKey = "status:" + previousStatus;
                Long prevCount = redisTemplate.opsForHash().increment(key, prevKey, -1);
                log.debug("  - {} 감소: {}", prevKey, prevCount);
            }

            // 새 상태 증가
            String newKey = "status:" + newStatus;
            Long newCount = redisTemplate.opsForHash().increment(key, newKey, 1);
            log.debug("  - {} 증가: {}", newKey, newCount);

            log.info("✅ 주문 상태 변경 완료 - branchId: {}, {} -> {}", branchId, previousStatus, newStatus);
        } catch (Exception e) {
            log.error("❌ 주문 상태 변경 캐시 업데이트 실패 - branchId: {}", branchId, e);
        }
    }

    /**
     * 캐시된 주문 데이터 조회
     */
    public Map<String, Object> getCachedOrderData(Long branchId) {
        try {
            String key = getOrderKey(branchId);
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (data.isEmpty()) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            data.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception e) {
            log.error("주문 캐시 조회 실패 - branchId: {}", branchId, e);
            return null;
        }
    }

    // ========== 재고 현황 캐시 ==========

    /**
     * 재고 통계 갱신 (재고 변동 시)
     */
    public void updateInventoryStats(Long branchId, Long totalProducts, Long lowStockProducts, Double fulfillmentRate) {
        try {
            String key = getInventoryKey(branchId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProducts", totalProducts);
            stats.put("lowStockProducts", lowStockProducts);
            stats.put("stockFulfillmentRate", fulfillmentRate);

            redisTemplate.opsForHash().putAll(key, stats);
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.debug("재고 통계 갱신 - branchId: {}", branchId);
        } catch (Exception e) {
            log.error("재고 통계 캐시 업데이트 실패 - branchId: {}", branchId, e);
        }
    }

    /**
     * 캐시된 재고 데이터 조회
     */
    public Map<String, Object> getCachedInventoryData(Long branchId) {
        try {
            String key = getInventoryKey(branchId);
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (data.isEmpty()) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            data.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        } catch (Exception e) {
            log.error("재고 캐시 조회 실패 - branchId: {}", branchId, e);
            return null;
        }
    }

    // ========== 캐시 무효화 ==========

    /**
     * 특정 지점의 모든 대시보드 캐시 삭제
     */
    public void invalidateCache(Long branchId) {
        try {
            redisTemplate.delete(getSalesKey(branchId));
            redisTemplate.delete(getOrderKey(branchId));
            redisTemplate.delete(getInventoryKey(branchId));
            log.info("대시보드 캐시 무효화 완료 - branchId: {}", branchId);
        } catch (Exception e) {
            log.error("캐시 무효화 실패 - branchId: {}", branchId, e);
        }
    }

    // ========== Helper Methods ==========

    private String getSalesKey(Long branchId) {
        return SALES_KEY_PREFIX + branchId;
    }

    private String getOrderKey(Long branchId) {
        return ORDER_KEY_PREFIX + branchId;
    }

    private String getInventoryKey(Long branchId) {
        return INVENTORY_KEY_PREFIX + branchId;
    }
}

