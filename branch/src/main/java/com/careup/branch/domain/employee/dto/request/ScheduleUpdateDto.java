package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateDto {

    @NotNull
    private ScheduleTypeCategory category;

    private Long workTypeId;

    private Long leaveTypeId;

    @NotNull
    private Long branchId;

    private Long attendanceTemplateId;

    @NotNull
    private LocalDate registeredDate;

    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;
}
