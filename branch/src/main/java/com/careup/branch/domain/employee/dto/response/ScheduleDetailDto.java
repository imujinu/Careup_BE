package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
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
public class ScheduleDetailDto {
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
    private Long attendanceTemplateId;
    private String attendanceTemplateName;

    private LocalDate registeredDate;
    private LocalDateTime registeredClockIn;
    private LocalDateTime registeredBreakStart;
    private LocalDateTime registeredBreakEnd;
    private LocalDateTime registeredClockOut;

    private LocalDateTime actualClockIn;
    private LocalDateTime actualBreakStart;
    private LocalDateTime actualBreakEnd;
    private LocalDateTime actualClockOut;

    private Integer actualWorkMinutes;
    private Integer actualBreakMinutes;

    private AttendanceStatus status;
    private String badgeText;
    private boolean missedCheckout;

    public static ScheduleDetailDto from(Schedule s, ScheduleEvent e) {
        return from(s, e, null, null);
    }

    public static ScheduleDetailDto from(Schedule s, ScheduleEvent e, AttendanceStatus status, String badgeText) {
        Integer workMin  = e != null ? e.getTotalWorkMinutes()  : null;
        Integer breakMin = e != null ? e.getTotalBreakMinutes() : null;

        return ScheduleDetailDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                .category(s.getCategory())
                .workTypeId(s.getWorkType() != null ? s.getWorkType().getId() : null)
                .workTypeName(s.getWorkType() != null ? s.getWorkType().getName() : null)
                .leaveTypeId(s.getLeaveType() != null ? s.getLeaveType().getId() : null)
                .leaveTypeName(s.getLeaveType() != null ? s.getLeaveType().getName() : null)
                .attendanceTemplateId(s.getAttendanceTemplate() != null ? s.getAttendanceTemplate().getId() : null)
                .attendanceTemplateName(s.getAttendanceTemplate() != null ? s.getAttendanceTemplate().getName() : null)
                .registeredDate(s.getRegisteredDate())
                .registeredClockIn(s.getRegisteredClockIn())
                .registeredBreakStart(s.getRegisteredBreakStart())
                .registeredBreakEnd(s.getRegisteredBreakEnd())
                .registeredClockOut(s.getRegisteredClockOut())
                .actualClockIn(e != null ? e.getClockInAt() : null)
                .actualBreakStart(e != null ? e.getBreakStartAt() : null)
                .actualBreakEnd(e != null ? e.getBreakEndAt() : null)
                .actualClockOut(e != null ? e.getClockOutAt() : null)
                .actualWorkMinutes(workMin)
                .actualBreakMinutes(breakMin)
                .status(status)
                .badgeText(badgeText)
                .missedCheckout(e != null && e.isMissedCheckout())
                .build();
    }
}
