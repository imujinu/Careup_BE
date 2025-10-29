package com.careup.branch.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자동 발주 히스토리 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoOrderHistoryDto {

    private Long autoOrderId;
    private Long branchId;
    private String branchName;
    private Long purchaseOrderId;
    private String reason; // 자동 발주 사유
    private String status; // 자동 발주 상태
    private LocalDateTime createdAt;
    private List<AutoOrderProductDto> products; // 상품별 자동 발주 내역

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutoOrderProductDto { // 자동 발주 상품 내역
        private Long productId;
        private String productName;
        private Long quantity;
        private Long currentStock;
        private Long safetyStock;
    }
}

