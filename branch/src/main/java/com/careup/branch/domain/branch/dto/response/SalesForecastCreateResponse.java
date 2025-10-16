package com.careup.branch.domain.branch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesForecastCreateResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private Long amount;
    private String message;
}
