package com.careup.branch.domain.employee.dto.request;

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

    // category는 더 이상 클라이언트에서 받지 않음
    private Long workTypeId;   // 근무로 전환/유지 시 사용
    private Long leaveTypeId;  // 휴가로 전환/유지 시 사용

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
