package com.careup.ordering.domain.member.dto.request;

import com.careup.ordering.domain.member.entity.LoyalCustomer;
import com.careup.ordering.domain.member.entity.LoyalGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyalCustomerUpdateRequest {
    
    private BigDecimal totalAmount;
    private Integer orderCount;
    private LoyalGrade grade;
}
