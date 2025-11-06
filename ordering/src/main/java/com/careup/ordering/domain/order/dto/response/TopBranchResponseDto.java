package com.careup.ordering.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopBranchResponseDto {
    private Long branchId;
    private String branchName;
    private Long totalSales;
    private Long totalOrders;
    private String month; // YYYY-MM
}



