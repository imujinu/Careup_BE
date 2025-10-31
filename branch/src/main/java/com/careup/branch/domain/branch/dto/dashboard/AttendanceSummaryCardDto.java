package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 출근 현황 카드
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryCardDto {
    private Map<LocalDate, AttendanceDayDto> weeklyAttendance;  // 주간 출근 현황 (7일)
    private Double averageAttendanceRate;  // 평균 출근률 (%)
    private Long totalWorkDays;            // 총 출근일
    private Long lateCount;                // 지각 횟수

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceDayDto {
        private Long presentCount;   // 출근 인원
        private Long absentCount;    // 결근 인원
        private Long totalCount;     // 총 인원
        private Double attendanceRate;  // 출근률 (%)
    }
}

