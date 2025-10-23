package com.careup.branch.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaborCostResponseDto {
    private double currentLaborCostRate;     // 현재 시간 인건비율 (%)
    private double avgLaborCostRatePrevWeek; // 전주 동일 시간 평균 인건비율 (%)
    private String recommendation;           // 근무 인원 조정 추천 메시지
    private String analysis;                 // AI 분석 메시지
}