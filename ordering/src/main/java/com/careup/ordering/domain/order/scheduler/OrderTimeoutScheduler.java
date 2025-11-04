package com.careup.ordering.domain.order.scheduler;

import com.careup.ordering.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주문 타임아웃 처리 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    /**
     * 매 30초마다 실행하여 타임아웃 주문 취소
     * 1분 이상 결제가 완료되지 않은 주문은 자동으로 취소하고 재고를 복구
     */
    @Scheduled(fixedRate = 30000) // 30초마다 실행
    public void cancelTimeoutOrders() {
        try {
            log.debug("타임아웃 주문 취소 작업 시작");
            int cancelledCount = orderService.cancelTimeoutOrders(1); // 1분 타임아웃
            
            if (cancelledCount > 0) {
                log.info("타임아웃 주문 취소 완료 - 총 {}개 주문 취소", cancelledCount);
            }
        } catch (Exception e) {
            log.error("타임아웃 주문 취소 중 오류 발생", e);
        }
    }
}

