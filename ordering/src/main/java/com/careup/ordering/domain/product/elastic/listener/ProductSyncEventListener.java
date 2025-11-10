package com.careup.ordering.domain.product.elastic.listener;

import com.careup.ordering.domain.product.elastic.document.ProductDocument;
import com.careup.ordering.domain.product.elastic.service.ProductSearchService;
import com.careup.ordering.domain.product.event.ProductSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 상품 동기화 이벤트를 수신하여 Elasticsearch에 반영하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class ProductSyncEventListener {

    private final ProductSearchService productSearchService;

    @KafkaListener(
            topics = "product-sync",
            groupId = "product-sync-group",
            containerFactory = "productSyncKafkaListenerContainerFactory"
    )
    public void handleProductSyncEvent(ProductSyncEvent event) {
        log.info("=== Received product sync event: {} for product ID: {} ===", event.getSyncType(), event.getProductId());
        log.debug("Event details - name: {}, categoryId: {}, price: {}",
                  event.getName(), event.getCategoryId(), event.getPrice());

        try {
            switch (event.getSyncType()) {
                case CREATE:
                case UPDATE:
                    saveOrUpdateProductDocument(event);
                    break;
                case DELETE:
                    deleteProductDocument(event.getProductId());
                    break;
                default:
                    log.warn("Unknown sync type: {}", event.getSyncType());
            }
        } catch (Exception e) {
            log.error("Error handling product sync event for product ID: {}", event.getProductId(), e);
        }
    }

    /**
     * Elasticsearch에 상품 문서 저장/업데이트
     */
    private void saveOrUpdateProductDocument(ProductSyncEvent event) {
        log.info("Saving/Updating product document in Elasticsearch - productId: {}", event.getProductId());

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

        log.debug("Document to save: id={}, name={}, categoryName={}",
                  document.getId(), document.getName(), document.getCategoryName());

        productSearchService.saveProductDocument(document);
        log.info("✅ Product document saved/updated in Elasticsearch successfully: {}", event.getProductId());
    }

    /**
     * Elasticsearch에서 상품 문서 삭제
     */
    private void deleteProductDocument(Long productId) {
        productSearchService.deleteProductDocument(productId);
        log.info("Product document deleted from Elasticsearch: {}", productId);
    }
}

