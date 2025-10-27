package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 프로모션이 적용된 상품 가격 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionPriceDto {
    
    private Long branchProductId;
    private String productName;
    private BigDecimal originalPrice;      // 원가
    private BigDecimal discountRate;       // 할인율 (%)
    private BigDecimal discountAmount;     // 할인 금액
    private BigDecimal finalPrice;         // 최종 가격
    private boolean hasPromotion;          // 프로모션 적용 여부
    private Long promotionId;              // 적용된 프로모션 ID
}
