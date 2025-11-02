package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequestDto {
    private Long branchProductId;
    private Long quantity;
    private String reason;
    private Long purchaseOrderId;
}


