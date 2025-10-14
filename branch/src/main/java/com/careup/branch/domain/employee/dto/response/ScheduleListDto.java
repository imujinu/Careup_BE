package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long scheduleTypeId;
    private String scheduleTypeName;
    private ScheduleTypeCategory scheduleTypeCategory;
    private LocalDate registeredDate;
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;
    private LocalDateTime actualClockIn;
    private LocalDateTime actualBreakStart;
    private LocalDateTime actualBreakEnd;
    private LocalDateTime actualClockOut;

    public static ScheduleListDto from(Schedule s, ScheduleEvent e) {
        return ScheduleListDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
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
                .build();
    }
}
