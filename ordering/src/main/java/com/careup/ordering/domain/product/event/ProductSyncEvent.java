package com.careup.ordering.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB와 Elasticsearch 간 상품 동기화 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSyncEvent {
    private Long productId;
    private String name;
    private String description;
    private String categoryName;
    private Long categoryId;
    private Long price;
    private String imageUrl;
    private String status;
    private String visibility;
    private SyncType syncType; // CREATE, UPDATE, DELETE

    public enum SyncType {
        CREATE,
        UPDATE,
        DELETE
    }
}

