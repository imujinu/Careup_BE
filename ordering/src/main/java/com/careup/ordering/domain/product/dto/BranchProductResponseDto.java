package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.BranchProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductResponseDto {
    private Long branchProductId;
    private Long productId;
    private String productName;
    private Long branchId;
    private String serialNumber;
    private Long stockQuantity;
    private Long safetyStock;
    private Long price;

    /**
     * Entity -> DTO 변환
     */
    public static BranchProductResponseDto from(BranchProduct branchProduct) {
        return BranchProductResponseDto.builder()
                .branchProductId(branchProduct.getId())
                .productId(branchProduct.getProduct().getId())
                .productName(branchProduct.getProduct().getName())
                .branchId(branchProduct.getBranchId())
                .serialNumber(branchProduct.getSerialNumber())
                .stockQuantity(branchProduct.getStockQuantity())
                .safetyStock(branchProduct.getSafetystock())
                .price(branchProduct.getPrice())
                .build();
    }
}
