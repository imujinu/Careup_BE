package com.careup.branch.domain.chat.dto.sales;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayrollAnalysisResDto {
    private Long branchId;
    private LocalDate date;
    private double avgLaborCostRatio;
    private List<HourlyPayrollDto> details;
    private String summaryMessage;
}