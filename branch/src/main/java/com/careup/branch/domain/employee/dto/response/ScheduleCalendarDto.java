package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
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
public class ScheduleCalendarDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate date;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String timeSource;

    public static ScheduleCalendarDto fromResolved(Schedule s, ScheduleEvent e) {
        boolean hasIn = e != null && e.getClockInAt() != null;
        boolean hasOut = e != null && e.getClockOutAt() != null;
        LocalDateTime start = hasIn ? e.getClockInAt() : s.getRegisteredClockIn();
        LocalDateTime end   = hasOut ? e.getClockOutAt() : s.getRegisteredClockOut();
        String source = hasIn && hasOut ? "ACTUAL" : (hasIn || hasOut ? "PARTIAL_ACTUAL" : "REGISTERED");
        String title = s.getScheduleType() != null ? s.getScheduleType().getName() : "Schedule";
        return ScheduleCalendarDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                .date(s.getRegisteredDate())
                .title(title)
                .startAt(start)
                .endAt(end)
                .timeSource(source)
                .build();
    }
}
