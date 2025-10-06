package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long categoryId;
        private String name;
        private String description;
        private Long minPrice;
        private Long maxPrice;
        private String imageUrl;
        private String visibility;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long productId;
        private String name;
        private String description;
        private Long minPrice;
        private Long maxPrice;
        private String imageUrl;
        private String status;
        private String visibility;
    }
}
