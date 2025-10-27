package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.Promotion;
import com.careup.ordering.domain.product.entity.PromotionStatus;
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
public class PromotionResponseDto {
    
    private Long promotionId;
    
    private Long branchProductId;
    
    private String productName;
    
    private BigDecimal discountRate;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private PromotionStatus status;
    
    public static PromotionResponseDto from(Promotion promotion) {
        return PromotionResponseDto.builder()
                .promotionId(promotion.getId())
                .branchProductId(promotion.getBranchProduct().getId())
                .productName(promotion.getBranchProduct().getProduct().getName())
                .discountRate(promotion.getDiscountRate())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .status(promotion.getStatus())
                .build();
    }
}
