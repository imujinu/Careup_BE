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

    private static final long LATE_MINUTES_THRESHOLD = 1;   // 지각 임계 (분)
    private static final long EARLY_MINUTES_THRESHOLD = 1;  // 조퇴 임계 (분)
    private static final long OVERTIME_GRACE_MINUTES = 30;   // 초과근무 그레이스 (분)

    private final ScheduleTimeService time;

    public AttendanceStatus resolve(Schedule s, ScheduleEvent e, LocalDateTime now) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) {
            return AttendanceStatus.LEAVE;
        }

        // 등록값 (자정 넘김 보정)
        LocalDateTime regInRaw  = s.getRegisteredClockIn();
        LocalDateTime regOutRaw = s.getRegisteredClockOut();
        LocalDateTime regIn  = regInRaw;
        LocalDateTime regOut = time.normalizeOut(regInRaw, regOutRaw);

        // 실제 이벤트
        LocalDateTime in  = e != null ? e.getClockInAt()    : null;
        LocalDateTime brS = e != null ? e.getBreakStartAt() : null;
        LocalDateTime brE = e != null ? e.getBreakEndAt()   : null;
        LocalDateTime out = e != null ? e.getClockOutAt()   : null;
        boolean missedFlag = e != null && e.isMissedCheckout();

        LocalDate today = now.toLocalDate();
        boolean pastDay    = s.getRegisteredDate() != null && s.getRegisteredDate().isBefore(today);
        boolean pastRegOut = regOut != null && now.isAfter(regOut);

        if (missedFlag) return AttendanceStatus.MISSED_CHECKOUT;

        // 퇴근 판정
        if (out != null) {
            if (regOut != null) {
                long afterMin = Duration.between(regOut, out).toMinutes(); // +면 등록보다 늦게 퇴근
                long earlyMin = Duration.between(out, regOut).toMinutes(); // +면 등록보다 일찍 퇴근
                if (afterMin >= OVERTIME_GRACE_MINUTES) return AttendanceStatus.OVERTIME;
                if (earlyMin >= EARLY_MINUTES_THRESHOLD) return AttendanceStatus.EARLY_LEAVE;
            }
            // 등록과 동일 또는 그레이스 이내 초과 ⇒ 정상 퇴근
            return AttendanceStatus.CLOCKED_OUT;
        }

        // 휴게 중
        if (brS != null && brE == null) return AttendanceStatus.ON_BREAK;

        // 근무 중
        if (in != null) {
            if (pastDay || pastRegOut) return AttendanceStatus.MISSED_CHECKOUT;
            if (regIn != null) {
                long late = Duration.between(regIn, in).toMinutes();
                if (late >= LATE_MINUTES_THRESHOLD) return AttendanceStatus.LATE;
            }
            return AttendanceStatus.CLOCKED_IN;
        }

        // 당일 지나갔는데 아무 기록 없음
        if (pastDay) return AttendanceStatus.ABSENT;

        // 예정
        return AttendanceStatus.PLANNED;
    }
}
