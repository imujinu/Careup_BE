package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "schedule")
public class Schedule extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="schedule_type_id", nullable = false)
    private ScheduleType scheduleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="schedule_template_id")
    private AttendanceTemplate attendanceTemplate;

    @Column(name = "registered_date", nullable = false)
    private LocalDate registeredDate;

    @Column(name = "registered_clock_in")
    private LocalDateTime registeredClockIn;

    @Column(name = "registered_break_start")
    private LocalDateTime registeredBreakStart;

    @Column(name = "registered_break_end")
    private LocalDateTime registeredBreakEnd;

    @Column(name = "registered_clock_out")
    private LocalDateTime registeredClockOut;

    public void change(
            ScheduleType type,
            AttendanceTemplate template,
            LocalDate date,
            LocalDateTime in,
            LocalDateTime breakStart,
            LocalDateTime breakEnd,
            LocalDateTime out
    ) {
        this.scheduleType = type;
        this.attendanceTemplate = template;
        this.registeredDate = date;
        this.registeredClockIn = in;
        this.registeredBreakStart = breakStart;
        this.registeredBreakEnd = breakEnd;
        this.registeredClockOut = out;
    }
}
