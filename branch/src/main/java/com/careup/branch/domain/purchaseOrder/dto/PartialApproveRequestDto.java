package com.careup.branch.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartialApproveRequestDto {
    
    private List<PartialApproveDetailDto> approvedDetails;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartialApproveDetailDto {
        private Long productId;
        private int approvedQuantity;
    }
}
