package com.careup.ordering.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponseDto {
    private Long cartId;
    private Long memberId;
    private Long branchProductId;
    private String productName;
    private String productImageUrl;
    private Long quantity;
    private Long unitPrice;
    private Long totalPrice;
    private String attributeName;
    private String attributeValue;
    private Long stockQuantity;
}
