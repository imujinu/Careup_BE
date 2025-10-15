package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.BranchProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long safetyStock;
    private Long price;
    private String productName;
    private String productDescription;

    /**
     * Entity → DTO 변환 정적 메서드
     */
    public static BranchProductResponseDto from(BranchProduct branchProduct) {
        return BranchProductResponseDto.builder()
                .branchProductId(branchProduct.getId())
                .productId(branchProduct.getProduct().getId())
                .branchId(branchProduct.getBranchId())
                .serialNumber(branchProduct.getSerialNumber())
                .stockQuantity(branchProduct.getStockQuantity())
                .safetyStock(branchProduct.getSafetystock())
                .price(branchProduct.getPrice())
                .productName(branchProduct.getProduct().getName())
                .productDescription(branchProduct.getProduct().getDescription())
                .build();
    }
}
