package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListDto {
    private Long id;

    // 직원
    private Long employeeId;
    private String employeeName;

    // 지점 (추가)
    private Long branchId;
    private String branchName;

    // 스케줄 타입
    private Long scheduleTypeId;
    private String scheduleTypeName;
    private ScheduleTypeCategory scheduleTypeCategory;

    // 계획(등록) 시간
    private LocalDate registeredDate;
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;

    // 실제(이벤트) 시간
    private LocalDateTime actualClockIn;
    private LocalDateTime actualBreakStart;
    private LocalDateTime actualBreakEnd;
    private LocalDateTime actualClockOut;

    // 합계
    private long totalBreakMinutes;
    private long totalWorkMinutes;

    public static ScheduleListDto of(Schedule s, ScheduleEvent e, long breakMin, long workMin) {
        return ScheduleListDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)                // 추가
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)            // 추가
                .scheduleTypeId(s.getScheduleType() != null ? s.getScheduleType().getId() : null)
                .scheduleTypeName(s.getScheduleType() != null ? s.getScheduleType().getName() : null)
                .scheduleTypeCategory(s.getScheduleType() != null ? s.getScheduleType().getCategory() : null)
                .registeredDate(s.getRegisteredDate())
                .registeredClockIn(s.getRegisteredClockIn())
                .registeredBreakStart(s.getRegisteredBreakStart())
                .registeredBreakEnd(s.getRegisteredBreakEnd())
                .registeredClockOut(s.getRegisteredClockOut())
                .actualClockIn(e != null ? e.getClockInAt() : null)
                .actualBreakStart(e != null ? e.getBreakStartAt() : null)
                .actualBreakEnd(e != null ? e.getBreakEndAt() : null)
                .actualClockOut(e != null ? e.getClockOutAt() : null)
                .totalBreakMinutes(Math.max(breakMin, 0))
                .totalWorkMinutes(Math.max(workMin, 0))
                .build();
    }
}
