package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
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
public class ScheduleCalendarDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private LocalDate date;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean allDay;
    private String timeSource;
    private AttendanceStatus status;
    private String badgeText;
    private boolean missedCheckout;
}
