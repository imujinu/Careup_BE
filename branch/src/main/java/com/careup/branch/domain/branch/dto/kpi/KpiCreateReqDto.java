package com.careup.branch.domain.branch.dto.kpi;

import com.careup.branch.domain.branch.entity.KPI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiCreateReqDto {

    private String name;
    private BigDecimal goal;
    private BigDecimal progress;

    // Entity -> DTO
    public static KpiCreateReqDto fromEntity(KPI kpi) {
        return KpiCreateReqDto.builder()
                .name(kpi.getName())
                .goal(kpi.getGoal())
                .progress(kpi.getProgress())
                .build();
    }
}
