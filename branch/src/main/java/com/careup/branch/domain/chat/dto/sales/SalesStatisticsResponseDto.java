package com.careup.branch.domain.chat.dto.sales;

import com.careup.branch.domain.chat.dto.SalesStatisticsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesStatisticsResponseDto {
    private Long branchId;
    private String periodType;
    private Long totalSales;
    private Long totalOrders;
    private List<SalesStatisticsDto> statistics;
}
