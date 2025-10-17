package com.careup.ordering.domain.order.dto.request;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HqSalesRequestDto {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String periodType; // DAY, WEEK, MONTH

    private List<Long> branchIds; // 선택한 가맹점 ID 목록 (비교용)

    // DTO에 필요한 데이터 삽입
    public static HqSalesRequestDto withDates(LocalDate startDate, LocalDate endDate, String periodType) {
        return HqSalesRequestDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .build();
    }

    public static HqSalesRequestDto withDatesAndBranchIds(
            List<Long> branchIds, LocalDate startDate, LocalDate endDate, String periodType) {
        return HqSalesRequestDto.builder()
                .branchIds(branchIds)
                .startDate(startDate)
                .endDate(endDate)
                .periodType(periodType)
                .build();
    }
}

