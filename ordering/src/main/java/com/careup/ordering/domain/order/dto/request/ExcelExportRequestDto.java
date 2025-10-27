package com.careup.ordering.domain.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 엑셀 내보내기 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelExportRequestDto {

    /**
     * 엑셀 타입: ALL_BRANCHES, BRANCH_DETAIL, BRANCH_COMPARISON, SALES_FORECAST
     */
    private String exportType;

    /**
     * 시작일
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 종료일
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 기간 타입: DAY, WEEK, MONTH
     */
    private String periodType;

    /**
     * 지점 ID (단일 지점 조회 시)
     */
    private Long branchId;

    /**
     * 비교할 지점 ID 목록 (지점 비교 시)
     */
    private List<Long> branchIds;

    /**
     * 예측 일수 (예상 매출액 조회 시)
     */
    private Integer forecastDays;
}

