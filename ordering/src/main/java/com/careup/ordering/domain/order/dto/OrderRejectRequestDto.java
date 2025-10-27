package com.careup.ordering.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderRejectRequestDto {
    @NotNull(message = "거부자 ID는 필수입니다")
    private Long rejectBy;

    @NotBlank(message = "거부 사유는 필수입니다")
    private String rejectedReason;
}
