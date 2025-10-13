package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BranchProductDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long productId;
        private Long branchId;
        private String serialNumber;
        private Long stockQuantity;
        private Long safetyStock;
        private Long price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long branchProductId;
        private Long productId;
        private Long branchId;
        private String serialNumber;
        private Long stockQuantity;
        private Long safetyStock;
        private Long price;
        private String productName;
        private String productDescription;
    }
}
