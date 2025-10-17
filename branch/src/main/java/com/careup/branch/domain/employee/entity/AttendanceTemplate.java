package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "attendance_template")
public class AttendanceTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column
    private LocalTime defaultClockIn;

    @Column
    private LocalTime defaultBreakStart;

    @Column
    private LocalTime defaultBreakEnd;

    @Column
    private LocalTime defaultClockOut;

    public void change(String name,
                       LocalTime defaultClockIn,
                       LocalTime defaultBreakStart,
                       LocalTime defaultBreakEnd,
                       LocalTime defaultClockOut) {
        this.name = name;
        this.defaultClockIn = defaultClockIn;
        this.defaultBreakStart = defaultBreakStart;
        this.defaultBreakEnd = defaultBreakEnd;
        this.defaultClockOut = defaultClockOut;
    }
}
