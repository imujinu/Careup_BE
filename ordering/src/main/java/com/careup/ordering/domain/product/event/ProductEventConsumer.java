package com.careup.ordering.domain.product.event;

// Elasticsearch 관련 import 주석처리
import com.careup.ordering.domain.product.elastic.document.ProductDocument;
import com.careup.ordering.domain.product.elastic.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class ProductEventConsumer {

    // Elasticsearch 관련 리포지토리 주석처리
    private final ProductSearchRepository productSearchRepository;

    @KafkaListener(topics = "product-events", groupId = "ordering-group")
    public void handleProductEvent(ProductEvent event) {
        try {
            log.info("Received product event: {} for product ID: {}", event.getEventType(), event.getProductId());

            switch (event.getEventType()) {
                case CREATED, UPDATED -> {
                    // Elasticsearch 관련 코드 주석처리
                    ProductDocument document = ProductDocument.builder()
                            .id(String.valueOf(event.getProductId()))
                            .productId(event.getProductId())
                            .name(event.getName())
                            .description(event.getDescription())
                            .categoryName(event.getCategoryName())
                            .categoryId(event.getCategoryId())
                            .price(event.getPrice())
                            .imageUrl(event.getImageUrl())
                            .status(event.getStatus())
                            .visibility(event.getVisibility())
                            .build();
                    productSearchRepository.save(document);
                    log.info("Product document saved to Elasticsearch: {}", event.getProductId());
                }
                case DELETED -> {
                    // Elasticsearch 관련 코드 주석처리
                    productSearchRepository.deleteById(String.valueOf(event.getProductId()));
                    log.info("Product document deleted from Elasticsearch: {}", event.getProductId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle product event", e);
        }
    }
}

