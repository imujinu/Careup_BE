package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "schedule",
        indexes = {
                @Index(name = "idx_schedule_registered_date", columnList = "registered_date"),
                @Index(name = "idx_schedule_employee_date",   columnList = "employee_id, registered_date")
        }
)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "branch_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedule_branch")
    )
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name ="employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedule_employee")
    )
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 10, nullable = false)
    private ScheduleTypeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name ="work_type_id",
            foreignKey = @ForeignKey(name = "fk_schedule_work_type")
    )
    private WorkType workType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name ="leave_type_id",
            foreignKey = @ForeignKey(name = "fk_schedule_leave_type")
    )
    private LeaveType leaveType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name ="schedule_template_id",
            foreignKey = @ForeignKey(name = "fk_schedule_template")
    )
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
            Branch branch,
            ScheduleTypeCategory category,
            WorkType workType,
            LeaveType leaveType,
            AttendanceTemplate template,
            LocalDate date,
            LocalDateTime in,
            LocalDateTime breakStart,
            LocalDateTime breakEnd,
            LocalDateTime out
    ) {
        this.branch = branch;
        this.category = category;
        this.workType = workType;
        this.leaveType = leaveType;
        this.attendanceTemplate = template;
        this.registeredDate = date;
        this.registeredClockIn = in;
        this.registeredBreakStart = breakStart;
        this.registeredBreakEnd = breakEnd;
        this.registeredClockOut = out;
    }

    public void changeSchedule(
            Branch branch,
            ScheduleTypeCategory category,
            WorkType workType,
            LeaveType leaveType,
            AttendanceTemplate template,
            LocalDate date,
            LocalDateTime in,
            LocalDateTime out,
            LocalDateTime breakStart,
            LocalDateTime breakEnd
    ) {
        this.branch = branch;
        this.category = category;
        this.workType = workType;
        this.leaveType = leaveType;
        this.attendanceTemplate = template;
        this.registeredDate = date;
        this.registeredClockIn = in;
        this.registeredClockOut = out;
        this.registeredBreakStart = breakStart;
        this.registeredBreakEnd = breakEnd;
    }
}
