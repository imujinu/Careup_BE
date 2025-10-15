package com.careup.branch.domain.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
