package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.BranchProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductResponseDto {
    private Long branchProductId;
    private Long productId;
    private Long branchId;
    private String serialNumber;
    private Long stockQuantity;
    private Long reservedQuantity;  // 예약재고 수량
    private Long availableQuantity;  // 사용 가능한 재고 수량 (실재고 - 예약재고)
    private Long safetyStock;
    private Long price;  // 원가
    private String productName;
    private String productDescription;
    private String categoryName;
    private Long categoryId;
    private Boolean hasPromotion;          // 프로모션 적용 여부
    private Long promotionId;              // 프로모션 ID
    private BigDecimal discountRate;       // 할인율 (%)
    private Long discountAmount;           // 할인 금액
    private Long finalPrice;               // 최종 판매가 (프로모션 적용)

    /**
     * Entity → DTO 변환 정적 메서드 (프로모션 정보 없음)
     */
    public static BranchProductResponseDto from(BranchProduct branchProduct) {
        return BranchProductResponseDto.builder()
                .branchProductId(branchProduct.getId())
                .productId(branchProduct.getProduct().getId())
                .branchId(branchProduct.getBranchId())
                .serialNumber(branchProduct.getSerialNumber())
                .stockQuantity(branchProduct.getStockQuantity())
                .reservedQuantity(branchProduct.getReservedQuantity())
                .availableQuantity(branchProduct.getAvailableQuantity())
                .safetyStock(branchProduct.getSafetystock())
                .price(branchProduct.getPrice())
                .productName(branchProduct.getProduct().getName())
                .productDescription(branchProduct.getProduct().getDescription())
                .categoryName(branchProduct.getProduct().getCategory() != null ?
                    branchProduct.getProduct().getCategory().getName() : null)
                .categoryId(branchProduct.getProduct().getCategory() != null ?
                    branchProduct.getProduct().getCategory().getId() : null)
                // 프로모션 정보 기본값
                .hasPromotion(false)
                .promotionId(null)
                .discountRate(BigDecimal.ZERO)
                .discountAmount(0L)
                .finalPrice(branchProduct.getPrice())  // 원가 그대로
                .build();
    }

    /**
     * Entity + 프로모션 정보 → DTO 변환 메서드
     */
    public static BranchProductResponseDto fromWithPromotion(
            BranchProduct branchProduct,
            PromotionPriceDto promotionPrice) {
        return BranchProductResponseDto.builder()
                .branchProductId(branchProduct.getId())
                .productId(branchProduct.getProduct().getId())
                .branchId(branchProduct.getBranchId())
                .serialNumber(branchProduct.getSerialNumber())
                .stockQuantity(branchProduct.getStockQuantity())
                .reservedQuantity(branchProduct.getReservedQuantity())
                .availableQuantity(branchProduct.getAvailableQuantity())
                .safetyStock(branchProduct.getSafetystock())
                .price(branchProduct.getPrice())
                .productName(branchProduct.getProduct().getName())
                .productDescription(branchProduct.getProduct().getDescription())
                // 프로모션 정보
                .hasPromotion(promotionPrice.isHasPromotion()) // 수정: getHasPromotion() → isHasPromotion()
                .promotionId(promotionPrice.getPromotionId())
                .discountRate(promotionPrice.getDiscountRate())
                .discountAmount(promotionPrice.getDiscountAmount().longValue())
                .finalPrice(promotionPrice.getFinalPrice().longValue())
                .build();
    }
}
