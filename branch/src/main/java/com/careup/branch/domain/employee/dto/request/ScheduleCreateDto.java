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
public class ScheduleCreateDto {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long branchId;

    // category는 더 이상 클라이언트에서 받지 않음
    private Long workTypeId;  // 근무 스케줄이면 필수(leaveTypeId는 null이어야 함)
    private Long leaveTypeId; // 휴가 스케줄이면 필수(workTypeId는 null이어야 함)

    private Long attendanceTemplateId;

    @NotNull
    private LocalDate registeredDate;

    // 단건 생성에서 시간을 직접 지정하는 경우(선택)
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;
}
