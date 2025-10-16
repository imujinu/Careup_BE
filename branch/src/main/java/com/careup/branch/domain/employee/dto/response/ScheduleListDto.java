package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
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
public class ScheduleListDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private ScheduleTypeCategory category;
    private Long workTypeId;
    private String workTypeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate registeredDate;
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;
    private LocalDateTime actualClockIn;
    private LocalDateTime actualBreakStart;
    private LocalDateTime actualBreakEnd;
    private LocalDateTime actualClockOut;
    private long totalBreakMinutes;
    private long totalWorkMinutes;

    // 표현 전용 필드(값만 담음, 계산은 Service에서)
    private AttendanceStatus status;
    private String badgeText;
}
