package com.careup.branch.domain.branch.dto.kpi;

import com.careup.branch.domain.branch.entity.kpi.KpiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchKpiCreateResDto {
    private Long id;
    private Long kpiId;
    private Long branchId;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private Double achievementRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private KpiStatus kpiStatus;
}
