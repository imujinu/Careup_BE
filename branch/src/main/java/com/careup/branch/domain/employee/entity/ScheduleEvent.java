package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    // 낙관적 락(중복 클릭/경합 방지)
    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", unique = true)
    private Schedule schedule;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "clock_in_at")
    private LocalDateTime clockInAt;

    @Column(name = "break_start_at")
    private LocalDateTime breakStartAt;

    @Column(name = "break_end_at")
    private LocalDateTime breakEndAt;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Column(name = "clock_in_lat", precision = 9, scale = 6)
    private BigDecimal clockInLat;

    @Column(name = "clock_in_lon", precision = 9, scale = 6)
    private BigDecimal clockInLon;

    @Column(name = "clock_out_lat", precision = 9, scale = 6)
    private BigDecimal clockOutLat;

    @Column(name = "clock_out_lon", precision = 9, scale = 6)
    private BigDecimal clockOutLon;

    @Column(name = "missed_checkout", nullable = false)
    private boolean missedCheckout;

    @Column(name = "total_work_minutes", nullable = false)
    private int totalWorkMinutes;

    @Column(name = "total_break_minutes", nullable = false)
    private int totalBreakMinutes;

    @Column(name = "attendance_status", nullable = true)
    private AttendanceStatus attendanceStatus;

    public void changeEventDate(LocalDate v) { this.eventDate = v; }
    public void changeClockIn(LocalDateTime v) { this.clockInAt = v; }
    public void changeBreakStart(LocalDateTime v) { this.breakStartAt = v; }
    public void changeBreakEnd(LocalDateTime v) { this.breakEndAt = v; }
    public void changeClockOut(LocalDateTime v) { this.clockOutAt = v; }
    public void markMissedCheckout() { this.missedCheckout = true; }
    public void clearMissedCheckout() { this.missedCheckout = false; }
    public void changeTotalWorkMinutes(int v) { this.totalWorkMinutes = Math.max(v, 0); }
    public void changeTotalBreakMinutes(int v) { this.totalBreakMinutes = Math.max(v, 0); }

    // ★ 위치 좌표 세터
    public void setClockInLatLon(BigDecimal lat, BigDecimal lon) {
        this.clockInLat = lat;
        this.clockInLon = lon;
    }
    public void setClockOutLatLon(BigDecimal lat, BigDecimal lon) {
        this.clockOutLat = lat;
        this.clockOutLon = lon;
    }
}
