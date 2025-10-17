package com.careup.branch.domain.branch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesForecastDto {

    private Long id;
    private Long branchId;
    private String branchName;
    private Long amount; // 예상 매출액
    private Date periodStart; // 시작 기간
    private Date periodEnd; // 종료 기간
    private Date createdAt; // 생성 일시
    private String forecastBasis; // 예측 근거
}

