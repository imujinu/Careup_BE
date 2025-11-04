package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 타임아웃 처리 스케줄러
 * 일정 시간(30분) 이상 PENDING 상태인 주문을 자동으로 취소합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // 결제 타임아웃 시간 (30분)
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;


    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다 실행 (밀리초 단위)
    @Transactional
    public void cancelTimeoutOrders() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

            // 30분 이상 PENDING 상태인 주문 조회
            List<Order> timeoutOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(
                    OrderStatus.PENDING,
                    timeoutThreshold
            );

            if (timeoutOrders.isEmpty()) {
                log.debug("타임아웃된 주문이 없습니다.");
                return;
            }

            log.info("타임아웃 주문 자동 취소 처리 시작 - {}건", timeoutOrders.size());

            int cancelledCount = 0;
            for (Order order : timeoutOrders) {
                try {
                    // 주문 취소 처리 (재고 복구 포함)
                    orderService.cancelOrder(order.getId());
                    cancelledCount++;
                    log.info("타임아웃 주문 자동 취소 완료 - orderId: {}, 생성시간: {}", 
                            order.getId(), order.getCreatedAt());
                } catch (IllegalStateException e) {
                    // 이미 승인된 주문은 취소할 수 없음
                    log.warn("타임아웃 주문 취소 실패 (이미 승인됨) - orderId: {}, error: {}", 
                            order.getId(), e.getMessage());
                } catch (Exception e) {
                    log.error("타임아웃 주문 취소 처리 중 오류 - orderId: {}", order.getId(), e);
                }
            }

            log.info("타임아웃 주문 자동 취소 처리 완료 - 총 {}건 중 {}건 취소됨", 
                    timeoutOrders.size(), cancelledCount);

        } catch (Exception e) {
            log.error("타임아웃 주문 자동 취소 스케줄러 실행 중 오류 발생", e);
        }
    }
}


