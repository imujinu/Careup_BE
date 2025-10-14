package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class ScheduleUpdateDto {
    @NotNull
    private Long scheduleTypeId;
    private Long attendanceTemplateId;
    @NotNull
    private Long branchId;
    @NotNull
    private LocalDate registeredDate;
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;
}
