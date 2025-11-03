package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
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
public class ScheduleEventDetailDto {
    private Long scheduleId;
    private Long eventId;         // ★ 추가
    private Long employeeId;
    private String employeeName;
    private LocalDate eventDate;
    private LocalDateTime clockInAt;
    private LocalDateTime breakStartAt;
    private LocalDateTime breakEndAt;
    private LocalDateTime clockOutAt;
    private boolean missedCheckout;
    private AttendanceStatus status;

    public static ScheduleEventDetailDto of(Schedule s, ScheduleEvent e, AttendanceStatus status) {
        return ScheduleEventDetailDto.builder()
                .scheduleId(s.getId())
                .eventId(e != null ? e.getId() : null) // ★ 추가
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                .eventDate(e != null ? e.getEventDate() : null)
                .clockInAt(e != null ? e.getClockInAt() : null)
                .breakStartAt(e != null ? e.getBreakStartAt() : null)
                .breakEndAt(e != null ? e.getBreakEndAt() : null)
                .clockOutAt(e != null ? e.getClockOutAt() : null)
                .missedCheckout(e != null && e.isMissedCheckout())
                .status(status)
                .build();
    }
}
