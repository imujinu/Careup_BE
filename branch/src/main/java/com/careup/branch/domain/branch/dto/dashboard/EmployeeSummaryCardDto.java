package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 직원 현황 카드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryCardDto {
    private Long totalEmployees;       // 총 직원 수
    private Long presentEmployees;     // 출근 중
    private Long absentEmployees;      // 결근/휴가
    private Double todayAttendanceRate; // 오늘 출근률 (%) - 차트용
}

