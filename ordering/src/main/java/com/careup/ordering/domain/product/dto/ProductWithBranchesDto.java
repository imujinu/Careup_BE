package com.careup.ordering.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWithBranchesDto {
    private Long productId;
    private String productName;
    private String description;
    private String imageUrl;
    private String categoryName;
    private Long minPrice;  // 최소 가격 추가
    private Long maxPrice;  // 최대 가격 추가
    private Integer availableBranchCount;
    private List<BranchInfoDto> availableBranches;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchInfoDto {
        private Long branchId;
        private String branchName;
        private Long stockQuantity;
        private Long price;  // 지점별 가격 추가
    }
}