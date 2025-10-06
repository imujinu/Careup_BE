package com.careup.ordering.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemResponseDto {
    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private String productName;
    private Long quantity;
    private Long unitPrice;
    private Long totalPrice;
}
