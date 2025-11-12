package com.careup.ordering.domain.order.kafka;

import com.careup.ordering.domain.order.event.OrderStatisticsEvent;
import com.careup.ordering.domain.order.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 주문 통계 이벤트 Consumer
 * Redis 캐시를 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatisticsConsumer {

    private final DashboardCacheService dashboardCacheService;

    @KafkaListener(
            topics = "order-statistics",
            groupId = "ordering-dashboard-group",
            containerFactory = "orderStatisticsKafkaListenerContainerFactory"
    )
    public void consumeOrderStatisticsEvent(OrderStatisticsEvent event) {
        try {
            log.info("📨 주문 통계 이벤트 수신 - orderId: {}, branchId: {}, eventType: {}, status: {}, amount: {}",
                    event.getOrderId(), event.getBranchId(), event.getEventType(),
                    event.getOrderStatus(), event.getTotalAmount());

            LocalDate orderDate = event.getOrderDateTime().toLocalDate();
            Long branchId = event.getBranchId();
            Long amount = event.getTotalAmount();
            String status = event.getOrderStatus();
            String previousStatus = event.getPreviousStatus();

            switch (event.getEventType()) {
                case "CREATE":
                    handleOrderCreate(branchId, amount, orderDate, status);
                    break;

                case "UPDATE":
                    handleOrderUpdate(branchId, amount, orderDate, previousStatus, status);
                    break;

                case "DELETE":
                    handleOrderDelete(branchId, amount, orderDate, status);
                    break;

                default:
                    log.warn("⚠️ 알 수 없는 이벤트 타입 - eventType: {}", event.getEventType());
            }

            log.info("✅ 주문 통계 이벤트 처리 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ 주문 통계 이벤트 처리 실패 - orderId: {}", event.getOrderId(), e);
        }
    }

    /**
     * 주문 생성 이벤트 처리
     */
    private void handleOrderCreate(Long branchId, Long amount, LocalDate orderDate, String status) {
        // 주문 상태 카운트 증가
        dashboardCacheService.incrementOrderStatus(branchId, status);

        // CONFIRMED 상태인 경우에만 매출 통계 증가
        if ("CONFIRMED".equals(status)) {
            dashboardCacheService.incrementSales(branchId, amount, orderDate);
        }

        log.debug("주문 생성 캐시 업데이트 완료 - branchId: {}, status: {}", branchId, status);
    }

    /**
     * 주문 상태 변경 이벤트 처리
     */
    private void handleOrderUpdate(Long branchId, Long amount, LocalDate orderDate, String previousStatus, String newStatus) {
        log.info("🔄 주문 상태 변경 처리 시작 - branchId: {}, previousStatus: {}, newStatus: {}, amount: {}",
                branchId, previousStatus, newStatus, amount);

        // 주문 상태 카운트 조정
        dashboardCacheService.updateOrderStatus(branchId, previousStatus, newStatus);

        // 이전: PENDING/CANCELLED -> 신규: CONFIRMED (매출 증가)
        if (!"CONFIRMED".equals(previousStatus) && "CONFIRMED".equals(newStatus)) {
            log.info("✅ 주문 승인 감지 - 매출 통계 증가: branchId={}, amount={}", branchId, amount);
            dashboardCacheService.incrementSales(branchId, amount, orderDate);
        }

        // 이전: CONFIRMED -> 신규: CANCELLED (매출 감소)
        if ("CONFIRMED".equals(previousStatus) && "CANCELLED".equals(newStatus)) {
            log.info("⚠️ 주문 취소 감지 - 매출 통계 감소: branchId={}, amount={}", branchId, amount);
            dashboardCacheService.decrementSales(branchId, amount, orderDate);
        }

        log.info("✅ 주문 상태 변경 캐시 업데이트 완료 - branchId: {}, {} -> {}", branchId, previousStatus, newStatus);
    }

    /**
     * 주문 삭제 이벤트 처리
     */
    private void handleOrderDelete(Long branchId, Long amount, LocalDate orderDate, String status) {
        // CONFIRMED 상태였다면 매출 감소
        if ("CONFIRMED".equals(status)) {
            dashboardCacheService.decrementSales(branchId, amount, orderDate);
        }

        log.debug("주문 삭제 캐시 업데이트 완료 - branchId: {}, status: {}", branchId, status);
    }
}

