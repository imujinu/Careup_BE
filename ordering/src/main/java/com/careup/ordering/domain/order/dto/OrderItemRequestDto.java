package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemRequestDto {
    @NotNull(message = "상품ID는 필수입니다")
    private Long productId;

    @Min(value = 1,message = "수량은 1개 이상이어야 합니다")
    private Long quantity;

    @NotNull(message = "단가는 필수입니다")
    private Long unitPrice;
}
