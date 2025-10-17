package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllBranchesSalesResponseDto {
    private String periodType;
    private Long totalSales; // 전체 기간 총 매출
    private Long totalOrders; // 전체 기간 총 주문
    private Integer totalBranchCount; // 총 지점 수
    private List<AllBranchesSalesDto> salesData; // 기간별 매출 데이터
}

