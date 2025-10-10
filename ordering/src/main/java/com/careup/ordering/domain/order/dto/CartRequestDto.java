package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequestDto {

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "상품 ID는 필수입니다")
    private Long branchProductId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    @NotNull(message = "수량은 필수입니다")
    private Long quantity;

    private String attributeName;
    private String attributeValue;
}