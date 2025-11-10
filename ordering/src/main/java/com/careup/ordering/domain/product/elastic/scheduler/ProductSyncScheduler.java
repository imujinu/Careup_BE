package com.careup.ordering.domain.product.elastic.scheduler;

import com.careup.ordering.domain.product.elastic.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 DB와 Elasticsearch 간 동기화를 실행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "product.sync.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class ProductSyncScheduler {

    private final ProductSyncService productSyncService;

    /**
     * 매일 새벽 3시에 전체 동기화 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "${product.sync.scheduler.cron:0 0 3 * * *}")
    public void scheduledSync() {
        log.info("Starting scheduled product synchronization");
        try {
            productSyncService.syncAllProducts();
            log.info("Scheduled product synchronization completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled product synchronization", e);
        }
    }
}
