package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEvent extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 근태 기록 1:1 근태
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    // 실제 날짜
    @Column(name = "event_date")
    private LocalDate eventDate;

    // 실제 출근 시간
    @Column(name = "clock_in_at")
    private LocalDateTime clockInAt;

    // 실제 휴게 시작
    @Column(name = "break_start_at")
    private LocalDateTime breakStartAt;

    // 실제 휴게 종료
    @Column(name = "break_end_at")
    private LocalDateTime breakEndAt;

    // 실제 퇴근 시간
    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    // 출근 위치 (위도, 경도) - BigDecimal로 정밀도 지정
    @Column(name = "clock_in_lat", precision = 9, scale = 6)
    private BigDecimal clockInLat;

    @Column(name = "clock_in_lon", precision = 9, scale = 6)
    private BigDecimal clockInLon;

    // 퇴근 위치 (위도, 경도) - BigDecimal로 정밀도 지정
    @Column(name = "clock_out_lat", precision = 9, scale = 6)
    private BigDecimal clockOutLat;

    @Column(name = "clock_out_lon", precision = 9, scale = 6)
    private BigDecimal clockOutLon;
}
