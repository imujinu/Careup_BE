package com.careup.branch.domain.purchaseOrder.dto;

import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 본사용 발주 통계 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HQStatisticsResponseDto {
    
    // 전체 발주 현황
    private OverallStatistics overallStatistics;
    
    // 상태별 통계
    private List<StatusStatistics> statusStatistics;
    
    // 지점별 통계 (상위 10개)
    private List<BranchStatistics> branchStatistics;
    
    // 상품별 통계 (상위 10개)
    private List<ProductStatistics> productStatistics;
    
    /**
     * 전체 발주 현황
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverallStatistics {
        private Long totalOrderCount;  // 총 발주 건수
        private Long totalOrderAmount;  // 총 발주 금액
        private Long totalApprovedAmount;  // 총 승인 금액
        private Double averageOrderAmount;  // 평균 발주 금액
        private Long pendingCount;   // 대기 중인 발주 건수
        private Double approvalRate;  // 승인율
        private Double rejectionRate;  // 반려율
        private Double partialApprovalRate;   // 부분승인율
    }
    
    /**
     * 상태별 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusStatistics {
        private OrderStatus status;   // 발주 상태
        private Long count;            // 해당 상태 건수
        private Long totalAmount;   // 해당 상태 총 금액
        private Double percentage;  // 전체 대비 비율
    }
    
    /**
     * 지점별 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchStatistics {
        private Long branchId;   // 지점 ID
        private String branchName;   // 지점명
        private Long orderCount;  // 발주 건수
        private Long totalAmount;  // 총 발주 금액
        private Double averageAmount;  // 평균 발주 금액
        private Long approvedCount;  // 승인된 발주 건수
        private Long rejectedCount;  // 반려된 발주 건수
        private Double approvalRate;   // 승인율
    }
    
    /**
     * 상품별 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductStatistics {
        private Long productId;  // 상품 ID
        private String productName;  // 상품명
        private Long totalQuantity;  // 총 발주 수량
        private Long approvedQuantity;  // 총 승인 수량
        private Long totalAmount;   // 총 발주 금액
        private Long orderCount;    // 발주 건수
        private Double approvalRate;  // 승인율
    }

    /**
     * 가맹점용 발주 통계
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FranchiseStatistics {
        private Long branchId;
        private Long totalOrders;
        private Long pendingOrders;
        private Long approvedOrders;
        private Long totalAmount;  // 총 발주 금액
        private Long approvedAmount;  // 승인된 금액
        private Double approvalRate;  // 승인율
    }
}