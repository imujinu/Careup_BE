package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartRequestDto {

    @NotNull(message = "지정 상품 ID는 필수입니다")
    private Long branchProductId;

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 1이상이여야 합니다")
    private Long quantity;

    private String attributeName;
    private String attributeValue;
}
