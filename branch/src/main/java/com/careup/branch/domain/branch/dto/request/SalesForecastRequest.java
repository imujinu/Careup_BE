package com.careup.branch.domain.branch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesForecastRequest {

    private Long branchId;

    private Long amount; // 예상 매출액

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date periodStart; // 시작 기간

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date periodEnd; // 종료 기간
}

