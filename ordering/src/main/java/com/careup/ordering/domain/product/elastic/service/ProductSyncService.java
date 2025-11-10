package com.careup.ordering.domain.product.elastic.service;

import com.careup.ordering.domain.product.elastic.document.ProductDocument;
import com.careup.ordering.domain.product.elastic.repository.ProductSearchRepository;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.event.ProductSyncEvent;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DB와 Elasticsearch 간 데이터 동기화 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final KafkaTemplate<String, ProductSyncEvent> productSyncKafkaTemplate;

    private static final String PRODUCT_SYNC_TOPIC = "product-sync";

    /**
     * DB와 Elasticsearch 간 전체 동기화
     * - DB에 있지만 ES에 없는 데이터 → CREATE
     * - DB와 ES 모두 있지만 내용이 다른 데이터 → UPDATE
     * - ES에만 있고 DB에 없는 데이터 → DELETE
     */
    @Transactional(readOnly = true)
    public void syncAllProducts() {
        log.info("Starting full product synchronization between DB and Elasticsearch");

        // 1. DB에서 모든 활성 상품 조회
        List<Product> dbProducts = productRepository.findAll().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        // 2. Elasticsearch에서 모든 상품 ID 조회
        Iterable<ProductDocument> esDocuments = productSearchRepository.findAll();
        Set<Long> esProductIds = convertToSet(esDocuments);

        log.info("DB products: {}, ES products: {}", dbProducts.size(), esProductIds.size());

        // 3. DB 상품을 순회하며 동기화 이벤트 발행
        for (Product product : dbProducts) {
            if (!esProductIds.contains(product.getId())) {
                // ES에 없는 경우 → CREATE
                publishSyncEvent(product, ProductSyncEvent.SyncType.CREATE);
            } else {
                // ES에 있는 경우 → UPDATE (변경 여부와 관계없이 최신 상태로 동기화)
                publishSyncEvent(product, ProductSyncEvent.SyncType.UPDATE);
                esProductIds.remove(product.getId()); // 처리된 ID 제거
            }
        }

        // 4. ES에만 남아있는 상품 → DELETE
        for (Long esProductId : esProductIds) {
            publishDeleteEvent(esProductId);
        }

        log.info("Full product synchronization completed");
    }

    /**
     * 특정 상품만 동기화
     */
    @Transactional(readOnly = true)
    public void syncProduct(Long productId) {
        log.info("Syncing single product: {}", productId);

        productRepository.findById(productId).ifPresentOrElse(
                product -> {
                    if (product.isActive()) {
                        boolean existsInEs = productSearchRepository.existsById(String.valueOf(productId));
                        ProductSyncEvent.SyncType syncType = existsInEs
                                ? ProductSyncEvent.SyncType.UPDATE
                                : ProductSyncEvent.SyncType.CREATE;
                        publishSyncEvent(product, syncType);
                    } else {
                        // 비활성 상품은 ES에서 삭제
                        publishDeleteEvent(productId);
                    }
                },
                () -> {
                    // DB에 없으면 ES에서도 삭제
                    publishDeleteEvent(productId);
                }
        );
    }

    /**
     * 상품 동기화 이벤트 발행
     */
    private void publishSyncEvent(Product product, ProductSyncEvent.SyncType syncType) {
        log.info("Preparing to publish sync event - syncType: {}, productId: {}, name: {}",
                 syncType, product.getId(), product.getName());

        ProductSyncEvent event = ProductSyncEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryName(product.getCategory().getName())
                .categoryId(product.getCategory().getId())
                .price(product.getSupplyPrice())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus().name())
                .visibility(product.getVisibility().name())
                .syncType(syncType)
                .build();

        try {
            productSyncKafkaTemplate.send(PRODUCT_SYNC_TOPIC, String.valueOf(product.getId()), event);
            log.info("✅ Published sync event successfully: {} for product ID: {}, name: {}",
                     syncType, product.getId(), product.getName());
        } catch (Exception e) {
            log.error("❌ Failed to publish sync event: {} for product ID: {}, error: {}",
                      syncType, product.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 삭제 이벤트 발행
     */
    private void publishDeleteEvent(Long productId) {
        ProductSyncEvent event = ProductSyncEvent.builder()
                .productId(productId)
                .syncType(ProductSyncEvent.SyncType.DELETE)
                .build();

        productSyncKafkaTemplate.send(PRODUCT_SYNC_TOPIC, String.valueOf(productId), event);
        log.info("Published delete event for product ID: {}", productId);
    }

    /**
     * Elasticsearch 문서를 상품 ID Set으로 변환
     */
    private Set<Long> convertToSet(Iterable<ProductDocument> documents) {
        return ((List<ProductDocument>) documents).stream()
                .map(ProductDocument::getProductId)
                .collect(Collectors.toSet());
    }
}

