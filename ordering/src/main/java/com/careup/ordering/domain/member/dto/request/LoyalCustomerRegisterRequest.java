package com.careup.ordering.domain.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyalCustomerRegisterRequest {
    
    private Long memberId;
    private Long branchId;
    private BigDecimal initialAmount;  // 초기 금액 (선택적)
    private Integer initialOrderCount; // 초기 주문 횟수 (선택적)
}
