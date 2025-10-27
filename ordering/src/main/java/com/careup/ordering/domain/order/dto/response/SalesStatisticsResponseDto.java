package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.SalesStatisticsDto;
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

