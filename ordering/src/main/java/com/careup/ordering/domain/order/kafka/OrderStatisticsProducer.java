package com.careup.ordering.domain.order.kafka;

import com.careup.ordering.domain.order.event.OrderStatisticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 주문 통계 이벤트 Producer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatisticsProducer {

    @Qualifier("orderStatisticsKafkaTemplate")
    private final KafkaTemplate<String, OrderStatisticsEvent> kafkaTemplate;
    private static final String TOPIC = "order-statistics";

    /**
     * 주문 통계 이벤트 발행
     */
    public void sendOrderStatisticsEvent(OrderStatisticsEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getBranchId()), event)
                    .addCallback(
                            result -> {
                                log.debug("주문 통계 이벤트 전송 성공 - orderId: {}, branchId: {}, eventType: {}",
                                        event.getOrderId(), event.getBranchId(), event.getEventType());
                            },
                            ex -> {
                                log.error("주문 통계 이벤트 전송 실패 - orderId: {}", event.getOrderId(), ex);
                            }
                    );
        } catch (Exception e) {
            log.error("주문 통계 이벤트 발행 중 오류 - orderId: {}", event.getOrderId(), e);
        }
    }
}

