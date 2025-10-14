package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

    public void changeEventDate(LocalDate v) { this.eventDate = v; }
    public void changeClockIn(LocalDateTime v) { this.clockInAt = v; }
    public void changeBreakStart(LocalDateTime v) { this.breakStartAt = v; }
    public void changeBreakEnd(LocalDateTime v) { this.breakEndAt = v; }
    public void changeClockOut(LocalDateTime v) { this.clockOutAt = v; }
    public void markMissedCheckout() { this.missedCheckout = true; }
    public void clearMissedCheckout() { this.missedCheckout = false; }
}
