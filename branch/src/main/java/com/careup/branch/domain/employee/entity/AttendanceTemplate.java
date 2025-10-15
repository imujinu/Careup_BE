package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "attendance_template",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_template_name", columnNames = {"name"})
        }
)
public class AttendanceTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name; // 유니크로 강화

    @Column
    private LocalTime defaultClockIn;

    @Column
    private LocalTime defaultBreakStart;

    @Column
    private LocalTime defaultBreakEnd;

    @Column
    private LocalTime defaultClockOut;
}
