package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMassItemDto {
    @NotNull
    private Long employeeId;
    @NotNull
    private Long branchId;
    @NotNull
    private Long scheduleTypeId;
    private Long attendanceTemplateId;
    @NotNull
    private LocalDate date;
    private LocalTime registeredClockInTime;
    private LocalTime registeredBreakStartTime;
    private LocalTime registeredBreakEndTime;
    private LocalTime registeredClockOutTime;
}
