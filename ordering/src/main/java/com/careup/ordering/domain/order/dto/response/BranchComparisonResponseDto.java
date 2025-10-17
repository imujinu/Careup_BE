package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.BranchSalesDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchComparisonResponseDto {
    private String periodType;
    private List<Long> branchIds;
    private Long totalSales; // 선택한 지점들의 총 매출
    private Map<Long, String> branchNames; // 지점 ID -> 지점명 매핑
    private List<BranchSalesDetailDto> comparisonData; // 지점별 비교 데이터
}
