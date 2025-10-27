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
    }
}