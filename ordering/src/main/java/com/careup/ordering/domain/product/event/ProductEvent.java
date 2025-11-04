package com.careup.ordering.domain.product.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {
    private EventType eventType;
    private Long productId;
    private String name;
    private String description;
    private String categoryName;
    private Long categoryId;
    private Long price;
    private String imageUrl;
    private String status;
    private String visibility;

    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}

