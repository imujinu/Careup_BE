package com.careup.branch.domain.chat.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyPayrollDto {
    private int hour;
    private long salesAmount;
    private double laborCost;
    private double laborCostRatio;
}

