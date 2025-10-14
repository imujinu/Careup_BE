package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMassBlockDto {
    @NotNull
    private Long branchId;
    @NotNull
    private Long scheduleTypeId;
    private Long attendanceTemplateId;
    @NotEmpty
    private List<Long> employeeIds;
    @NotEmpty
    private List<LocalDate> dates;
    private LocalTime registeredClockInTime;
    private LocalTime registeredBreakStartTime;
    private LocalTime registeredBreakEndTime;
    private LocalTime registeredClockOutTime;
}
