package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceStatusResolver {

    private static final long LATE_MINUTES_THRESHOLD = 1;
    private static final long EARLY_MINUTES_THRESHOLD = 1;

    private final ScheduleTimeService time;

    public AttendanceStatus resolve(Schedule s, ScheduleEvent e, LocalDateTime now) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) {
            return AttendanceStatus.LEAVE;
        }

        LocalDateTime regInRaw  = s.getRegisteredClockIn();
        LocalDateTime regOutRaw = s.getRegisteredClockOut();
        LocalDateTime regIn  = regInRaw;
        LocalDateTime regOut = time.normalizeOut(regInRaw, regOutRaw);

        LocalDateTime in  = e != null ? e.getClockInAt()    : null;
        LocalDateTime brS = e != null ? e.getBreakStartAt() : null;
        LocalDateTime brE = e != null ? e.getBreakEndAt()   : null;
        LocalDateTime out = e != null ? e.getClockOutAt()   : null;
        boolean missedFlag = e != null && e.isMissedCheckout();

        LocalDate today = now.toLocalDate();
        boolean pastDay    = s.getRegisteredDate() != null && s.getRegisteredDate().isBefore(today);
        boolean pastRegOut = regOut != null && now.isAfter(regOut);

        if (missedFlag) return AttendanceStatus.MISSED_CHECKOUT;

        if (out != null) {
            if (regOut != null) {
                if (out.isAfter(regOut)) return AttendanceStatus.OVERTIME;
                long early = Duration.between(out, regOut).toMinutes();
                if (early >= EARLY_MINUTES_THRESHOLD) return AttendanceStatus.EARLY_LEAVE;
            }
            return AttendanceStatus.CLOCKED_OUT;
        }

        if (brS != null && brE == null) return AttendanceStatus.ON_BREAK;

        if (in != null) {
            if (pastDay || pastRegOut) return AttendanceStatus.MISSED_CHECKOUT;
            if (regIn != null) {
                long late = Duration.between(regIn, in).toMinutes();
                if (late >= LATE_MINUTES_THRESHOLD) return AttendanceStatus.LATE;
            }
            return AttendanceStatus.CLOCKED_IN;
        }

        if (pastDay) return AttendanceStatus.ABSENT;

        return AttendanceStatus.PLANNED;
    }
}
