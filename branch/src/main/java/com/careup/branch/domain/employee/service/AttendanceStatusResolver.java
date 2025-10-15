package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AttendanceStatusResolver {

    private static final long LATE_MINUTES_THRESHOLD = 1;
    private static final long EARLY_MINUTES_THRESHOLD = 1;

    private final ScheduleTimeService time;

    public AttendanceStatus resolve(Schedule s, ScheduleEvent e, LocalDateTime now) {
        // 0) 휴가/휴무 스케줄은 즉시 처리
        if (s.getScheduleType() != null
                && s.getScheduleType().getCategory() == ScheduleTypeCategory.LEAVE) {
            // 기획에 맞게 상태가 다르면 여기서 변경
            return AttendanceStatus.LEAVE;
        }

        LocalDateTime regInRaw  = s.getRegisteredClockIn();
        LocalDateTime regOutRaw = s.getRegisteredClockOut();

        LocalDateTime regIn  = regInRaw;
        LocalDateTime regOut = time.normalizeOut(regInRaw, regOutRaw);

        LocalDateTime in  = e != null ? e.getClockInAt()   : null;
        LocalDateTime brS = e != null ? e.getBreakStartAt(): null;
        LocalDateTime brE = e != null ? e.getBreakEndAt()  : null;
        LocalDateTime out = e != null ? e.getClockOutAt()  : null;
        boolean missedFlag = e != null && e.isMissedCheckout();

        if (missedFlag) return AttendanceStatus.MISSED_CHECKOUT;

        if (out != null) {
            if (regOut != null) {
                if (out.isAfter(regOut)) return AttendanceStatus.OVERTIME;
                long early = Duration.between(out, regOut).toMinutes();
                if (early >= EARLY_MINUTES_THRESHOLD) return AttendanceStatus.EARLY_LEAVE;
            }
            return AttendanceStatus.CLOCKED_OUT;
        }

        if (brS != null && brE == null) {
            return AttendanceStatus.ON_BREAK;
        }

        if (in != null) {
            if (regIn != null) {
                long late = Duration.between(regIn, in).toMinutes();
                if (late >= LATE_MINUTES_THRESHOLD) return AttendanceStatus.LATE;
            }
            return AttendanceStatus.CLOCKED_IN;
        }

        if (regOut != null && now.isBefore(regOut)) {
            return AttendanceStatus.PLANNED;
        }

        LocalDate today = now.toLocalDate();
        if (s.getRegisteredDate() != null && s.getRegisteredDate().isBefore(today)) {
            return AttendanceStatus.MISSED_CHECKOUT;
        }

        return AttendanceStatus.PLANNED;
    }
}
