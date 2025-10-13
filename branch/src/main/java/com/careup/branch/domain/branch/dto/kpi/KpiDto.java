package com.careup.branch.domain.branch.dto.kpi;

import com.careup.branch.domain.branch.entity.kpi.KPI;
import com.careup.branch.domain.branch.entity.kpi.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiDto {

    private Long id;
    private String name;
    private String description;
    private String category;
    private PeriodType periodType;
    private String calculationFormula;

    // Entity -> DTO
    public static KpiDto fromEntity(KPI kpi) {
        return KpiDto.builder()
                .id(kpi.getId())
                .name(kpi.getName())
                .description(kpi.getDescription())
                .category(kpi.getCategory())
                .periodType(kpi.getPeriodType())
                .calculationFormula(kpi.getCalculationFormula())
                .build();
    }
}
