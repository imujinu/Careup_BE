package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import org.springframework.stereotype.Component;

@Component
public class AttendanceBadgeResolver {

    public String toBadgeText(Schedule s, AttendanceStatus status) {
        if (status == null) {
            return plannedFallback(s);
        }
        switch (status) {
            case PLANNED:         return "예정";
            case LEAVE:           return s.getLeaveType() != null ? s.getLeaveType().getName() : "휴가";
            case LATE:            return "지각";
            case CLOCKED_IN:      return "근무중";
            case ON_BREAK:        return "휴게중";
            case EARLY_LEAVE:     return "조퇴";
            case CLOCKED_OUT:     return "근무완료";
            case OVERTIME:        return "초과근무";
            case MISSED_CHECKOUT: return "퇴근누락";
            case ABSENT:          return "결근";
            default:              return status.name();
        }
    }

    private String plannedFallback(Schedule s) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) {
            return s.getLeaveType() != null ? s.getLeaveType().getName() : "휴가";
        }
        return "예정";
    }
}
