package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.BranchSalesSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSalesSummaryResponseDto {
    private String periodType; // WEEK, MONTH, YEAR
    private Long totalSales; // 전체 지점 총 매출
    private Integer totalBranchCount; // 총 지점 수
    private List<BranchSalesSummaryDto> branches; // 지점별 매출 정보
}

