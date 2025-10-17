package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMassBlockDto {

    @NotNull
    private Long branchId;

    // category 제거. 서버가 workTypeId/leaveTypeId 존재로 판정
    private Long workTypeId;
    private Long leaveTypeId;

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
