package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class InventoryFlowDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long branchProductId;
        private Long inQuantity;
        private Long outQuantity;
        private String remark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long flowId;
        private Long branchProductId;
        private Long productId;
        private String productName;
        private Long branchId;
        private Long inQuantity;
        private Long outQuantity;
        private String remark;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyStockRequest {
        private Long branchProductId;
        private Long safetyStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockAdjustRequest {
        private Long branchProductId;
        private Long quantity;
        private String type;
        private String reason;
    }
}
