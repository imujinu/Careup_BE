package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private ScheduleTypeCategory category;

    private Long workTypeId;

    private Long leaveTypeId;

    private Long attendanceTemplateId;

    @NotNull
    private LocalDate date;

    private LocalTime registeredClockInTime;

    private LocalTime registeredBreakStartTime;

    private LocalTime registeredBreakEndTime;

    private LocalTime registeredClockOutTime;
}
