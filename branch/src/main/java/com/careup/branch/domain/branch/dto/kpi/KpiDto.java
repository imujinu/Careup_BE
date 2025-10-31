package com.careup.branch.domain.branch.dto.kpi;

import com.careup.branch.domain.branch.entity.kpi.KPI;
import com.careup.branch.domain.branch.entity.kpi.KpiCategory;
import com.careup.branch.domain.branch.entity.kpi.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiDto {

    private Long id;
    private String name;
    private String description;
    private KpiCategory category;
    private PeriodType periodType;
    private String calculationFormula;

    // KPI 목록에 필요한 실제 데이터
    private BigDecimal currentValue; // 현재값 (모든 지점의 평균 또는 합계)
    private BigDecimal targetValue; // 목표값 (모든 지점의 평균 또는 합계)
    private Double achievementRate; // 진행도(%) (전체 평균 달성률)
    private LocalDateTime createdAt; // 생성일시

    // Entity -> DTO (기본)
    public static KpiDto fromEntity(KPI kpi) {
        return KpiDto.builder()
                .id(kpi.getId())
                .name(kpi.getName())
                .description(kpi.getDescription())
                .category(kpi.getCategory())
                .periodType(kpi.getPeriodType())
                .calculationFormula(kpi.getCalculationFormula())
                .createdAt(kpi.getCreatedAt())
                .build();
    }

    // Entity -> DTO (BranchKpi 집계 데이터 포함)
    public static KpiDto fromEntityWithAggregation(KPI kpi,
                                                   BigDecimal currentValue,
                                                   BigDecimal targetValue,
                                                   Double achievementRate) {
        return KpiDto.builder()
                .id(kpi.getId())
                .name(kpi.getName())
                .description(kpi.getDescription())
                .category(kpi.getCategory())
                .periodType(kpi.getPeriodType())
                .calculationFormula(kpi.getCalculationFormula())
                .createdAt(kpi.getCreatedAt())
                .currentValue(currentValue != null ? currentValue : BigDecimal.ZERO)
                .targetValue(targetValue != null ? targetValue : BigDecimal.ZERO)
                .achievementRate(achievementRate != null ? achievementRate : 0.0)
                .build();
    }
}
