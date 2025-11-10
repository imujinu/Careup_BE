package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 출근 현황 카드
 * - WEEKLY: 최근 7일간 데이터
 * - MONTHLY: 최근 7개월간 데이터
 * - YEARLY: 최근 7년간 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryCardDto {
    private String period;                         // 조회 기간 (WEEKLY, MONTHLY, YEARLY)
    private List<ChartDataDto> chartData;          // 차트용 데이터 (순서 보장)
    private Double averageAttendanceRate;          // 평균 출근률 (%)
    private Long totalWorkDays;                    // 총 근무일
    private Long lateCount;                        // 지각 횟수

    /**
     * 차트 데이터 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataDto {
        private String label;            // 차트 라벨 (WEEKLY: "11/04", MONTHLY: "2025-04", YEARLY: "2019")
        private Long presentCount;       // 출근 인원
        private Long absentCount;        // 결근 인원
        private Long totalCount;         // 총 인원
        private Double attendanceRate;   // 출근률 (%)
    }
}

