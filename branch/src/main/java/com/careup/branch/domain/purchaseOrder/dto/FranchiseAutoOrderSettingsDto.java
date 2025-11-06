package com.careup.branch.domain.purchaseOrder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 가맹점 자동 발주 설정 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseAutoOrderSettingsDto {

    private Long branchId;
    private Boolean autoOrderEnabled; // 자동 발주 활성화 여부
    private List<ProductAutoOrderSettingDto> products; // 상품별 자동 발주 설정
    private LocalDateTime createdAt; // 설정 생성일시
    private LocalDateTime updatedAt; // 설정 수정일시

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    // 상품별 자동 발주 설정
    public static class ProductAutoOrderSettingDto {
        private Long productId;
        private Long branchProductId;
        private String productName;
        private Boolean autoOrderEnabled;
        private Long safetyStock;
        private Long currentStock;
    }
}

