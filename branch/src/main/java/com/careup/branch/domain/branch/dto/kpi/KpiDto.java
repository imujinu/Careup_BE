package com.careup.branch.domain.branch.dto.kpi;

import com.careup.branch.domain.branch.entity.kpi.KPI;
import com.careup.branch.domain.branch.entity.kpi.KpiCategory;
import com.careup.branch.domain.branch.entity.kpi.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // 추가 통계 정보
    private Long totalBranchCount; // 이 KPI를 사용하는 전체 지점 수
    private Long activeBranchCount; // 진행중인 지점 수
    private Long achievedBranchCount; // 달성한 지점 수
    private Double averageAchievementRate; // 평균 달성률
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

    // Entity -> DTO (통계 포함)
    public static KpiDto fromEntityWithStats(KPI kpi, Long totalBranchCount,
                                             Long activeBranchCount,
                                             Long achievedBranchCount,
                                             Double averageAchievementRate) {
        return KpiDto.builder()
                .id(kpi.getId())
                .name(kpi.getName())
                .description(kpi.getDescription())
                .category(kpi.getCategory())
                .periodType(kpi.getPeriodType())
                .calculationFormula(kpi.getCalculationFormula())
                .createdAt(kpi.getCreatedAt())
                .totalBranchCount(totalBranchCount != null ? totalBranchCount : 0L)
                .activeBranchCount(activeBranchCount != null ? activeBranchCount : 0L)
                .achievedBranchCount(achievedBranchCount != null ? achievedBranchCount : 0L)
                .averageAchievementRate(averageAchievementRate != null ? averageAchievementRate : 0.0)
                .build();
    }
}
