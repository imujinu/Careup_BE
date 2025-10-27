package com.careup.branch.domain.branch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkSalesForecastRequest {

    private List<SalesForecastRequest> forecasts; // 여러 지점의 예상 매출액
}

