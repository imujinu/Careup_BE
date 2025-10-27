package com.careup.branch.domain.branch.dto.response;

import com.careup.branch.domain.branch.dto.SalesForecastDto;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class
SalesForecastResponse {

    private Long branchId;
    private String branchName;
    private SalesForecastDto currentForecast; // 현재 적용 중인 예상 매출
    private List<SalesForecastDto> forecastHistory; // 예상 매출 이력
}

