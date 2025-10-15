package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name ="schedule_type_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedule_type")
    )
    private ScheduleType scheduleType;

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

    /** 지점 포함 전체 변경 */
    public void change(
            Branch branch,
            ScheduleType type,
            AttendanceTemplate template,
            LocalDate date,
            LocalDateTime in,
            LocalDateTime breakStart,
            LocalDateTime breakEnd,
            LocalDateTime out
    ) {
        this.branch = branch;
        this.scheduleType = type;
        this.attendanceTemplate = template;
        this.registeredDate = date;
        this.registeredClockIn = in;
        this.registeredBreakStart = breakStart;
        this.registeredBreakEnd = breakEnd;
        this.registeredClockOut = out;
    }
}
