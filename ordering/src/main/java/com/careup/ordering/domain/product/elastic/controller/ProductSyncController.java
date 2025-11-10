package com.careup.ordering.domain.product.elastic.controller;

import com.careup.ordering.domain.product.elastic.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 상품 동기화 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/admin/products/sync")
@RequiredArgsConstructor
public class ProductSyncController {

    private final ProductSyncService productSyncService;

    /**
     * 전체 상품 동기화 (DB → Elasticsearch)
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> syncAllProducts() {
        log.info("Manual full product synchronization triggered");

        try {
            productSyncService.syncAllProducts();

            Map<String, String> response = new HashMap<>();
            response.put("message", "전체 상품 동기화가 시작되었습니다.");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during manual product synchronization", e);

            Map<String, String> response = new HashMap<>();
            response.put("message", "동기화 중 오류가 발생했습니다: " + e.getMessage());
            response.put("status", "error");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 상품 동기화
     */
    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, String>> syncProduct(@PathVariable Long productId) {
        log.info("Manual product synchronization triggered for product ID: {}", productId);

        try {
            productSyncService.syncProduct(productId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "상품 ID " + productId + " 동기화가 완료되었습니다.");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing product ID: {}", productId, e);

            Map<String, String> response = new HashMap<>();
            response.put("message", "동기화 중 오류가 발생했습니다: " + e.getMessage());
            response.put("status", "error");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}

