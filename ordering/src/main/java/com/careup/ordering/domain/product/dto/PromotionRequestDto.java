package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequestDto {
    
    private Long branchProductId;
    
    private BigDecimal discountRate;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
}
