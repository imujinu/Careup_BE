package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderApproveRequestDto {
    @NotNull(message = "승인자 ID는 필수입니다")
    private Long approveBy;
}
