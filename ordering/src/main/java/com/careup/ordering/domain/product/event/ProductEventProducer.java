package com.careup.ordering.domain.product.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class ProductEventProducer {

    private static final String TOPIC = "product-events";
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public void sendProductEvent(ProductEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getProductId()), event);
            log.info("Product event sent: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to send product event", e);
        }
    }
}

