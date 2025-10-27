package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import org.springframework.stereotype.Component;

@Component
public class AttendanceBadgeResolver {

    public String toBadgeText(Schedule s, AttendanceStatus status) {
        if (status == null) {
            return plannedOrLeaveName(s);
        }
        switch (status) {
            case PLANNED:
                return plannedOrLeaveName(s);
            case LEAVE:
                return s.getLeaveType() != null ? s.getLeaveType().getName() : "Leave";
            default:
                return status.name(); // LATE, CLOCKED_IN, ON_BREAK, EARLY_LEAVE, CLOCKED_OUT, OVERTIME, MISSED_CHECKOUT, ABSENT
        }
    }

    private String plannedOrLeaveName(Schedule s) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) {
            return s.getLeaveType() != null ? s.getLeaveType().getName() : "Leave";
        }
        return s.getWorkType() != null ? s.getWorkType().getName() : "Work";
    }
}
