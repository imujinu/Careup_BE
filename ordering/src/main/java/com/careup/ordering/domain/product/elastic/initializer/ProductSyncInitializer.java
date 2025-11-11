package com.careup.ordering.domain.product.elastic.initializer;

import com.careup.ordering.domain.product.elastic.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 DB와 Elasticsearch 간 초기 동기화를 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.sync.initial.enabled", havingValue = "true")
public class ProductSyncInitializer implements ApplicationRunner {

    private final ProductSyncService productSyncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("Starting initial product synchronization");
        log.info("========================================");

        try {
            // Elasticsearch 연결 대기 (최대 30초)
            Thread.sleep(5000);

            productSyncService.syncAllProducts();

            log.info("========================================");
            log.info("✅ Initial product synchronization completed successfully");
            log.info("========================================");
        } catch (Exception e) {
            log.error("========================================");
            log.error("❌ Error during initial product synchronization: {}", e.getMessage(), e);
            log.error("========================================");
            // 초기 동기화 실패는 애플리케이션 시작을 막지 않음
        }
    }
}

