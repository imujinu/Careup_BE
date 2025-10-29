package com.careup.branch.domain.chat.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSalesDetailResponseDto {
    private Long branchId;
    private String branchName;
    private String periodType;
    private Long totalSales;
    private Long totalOrders;
    private Double marketShare; // 전체 대비 점유율
    private Integer ranking; // 전체 지점 중 순위
    private List<BranchSalesDetailDto> salesData; // 기간별 상세 데이터
}

