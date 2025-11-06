package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 매출 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesDto {
    private LocalDate date;
    private Long sales;
}

