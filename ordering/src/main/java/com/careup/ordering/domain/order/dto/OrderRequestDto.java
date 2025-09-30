package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderRequestDto {
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "지점ID는 필수입니다.")
    private Long branchId;
}
