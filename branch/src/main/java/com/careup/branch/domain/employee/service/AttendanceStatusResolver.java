package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.config.AttendanceWindowProperties;
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

    private static final long LATE_MINUTES_THRESHOLD = 1;   // 지각 허용치(분)
    private static final long EARLY_MINUTES_THRESHOLD = 1;  // 조퇴 허용치(분)

    private final ScheduleTimeService time;
    private final AttendanceWindowProperties windowProps;   // overtimeGraceMinutes 사용

    public AttendanceStatus resolve(Schedule s, ScheduleEvent e, LocalDateTime now) {
        // 휴가
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) return AttendanceStatus.LEAVE;

        // 계획(등록) 시간들 — 야간 보정 포함
        LocalDateTime regIn  = s.getRegisteredClockIn();
        LocalDateTime regOut = time.normalizeOut(regIn, s.getRegisteredClockOut());
        LocalDateTime planBrS = s.getRegisteredBreakStart();
        LocalDateTime planBrE = time.normalizeOut(planBrS, s.getRegisteredBreakEnd());

        // 실제 기록 시간들
        LocalDateTime in  = e != null ? e.getClockInAt()    : null;
        LocalDateTime brS = e != null ? e.getBreakStartAt() : null;
        LocalDateTime brE = e != null ? e.getBreakEndAt()   : null;
        LocalDateTime out = e != null ? e.getClockOutAt()   : null;

        boolean missedFlag = e != null && e.isMissedCheckout();

        LocalDate today = now.toLocalDate();
        boolean pastDay = s.getRegisteredDate() != null && s.getRegisteredDate().isBefore(today);
        boolean pastRegOut = regOut != null && now.isAfter(regOut);

        // 강제 표시 플래그가 있으면 우선
        if (missedFlag) return AttendanceStatus.MISSED_CHECKOUT;

        // 퇴근까지 찍은 경우: 초과/조퇴/완료
        if (out != null && in != null) {
            if (regOut != null) {
                long earlyMin = Duration.between(out, regOut).toMinutes();
                if (earlyMin >= EARLY_MINUTES_THRESHOLD) return AttendanceStatus.EARLY_LEAVE;
            }
            // 초과근무: (기록 순근무) >= (계획 순근무 + 그레이스)
            long recWorkMin  = minutesBetween(in, out);
            long recBreakMin = (e != null) ? e.getTotalBreakMinutes() : 0;
            long recNet      = Math.max(recWorkMin - recBreakMin, 0);

            long planWorkMin  = (regIn != null && regOut != null) ? minutesBetween(regIn, regOut) : 0;
            long planBreakMin = (planBrS != null && planBrE != null) ? minutesBetween(planBrS, planBrE) : 0;
            long planNet      = Math.max(planWorkMin - planBreakMin, 0);

            int grace = Math.max(windowProps.getOvertimeGraceMinutes(), 0);
            if (regIn != null && regOut != null && recNet >= planNet + grace) {
                return AttendanceStatus.OVERTIME;
            }
            return AttendanceStatus.CLOCKED_OUT;
        }

        // 휴게 시작만 찍혀 있으면 '휴게중'
        if (brS != null && brE == null) return AttendanceStatus.ON_BREAK;

        // 출근은 했는데 아직 퇴근 전
        if (in != null && out == null) {
            // ✅ 야간 대응: 계획 퇴근(regOut)이 있으면 그 시각을 넘긴 뒤에만 미퇴근 처리
            if (regOut != null) {
                if (now.isAfter(regOut)) return AttendanceStatus.MISSED_CHECKOUT;
            } else {
                // 계획 퇴근 자체가 없을 때만 자정 경과로 미퇴근 처리
                if (pastDay) return AttendanceStatus.MISSED_CHECKOUT;
            }
            // 지각/근무중
            if (regIn != null) {
                long late = Duration.between(regIn, in).toMinutes();
                if (late >= LATE_MINUTES_THRESHOLD) return AttendanceStatus.LATE;
            }
            return AttendanceStatus.CLOCKED_IN;
        }

        // 계획은 있는데 아직 출근 전
        if (regIn != null) {
            // ✅ 야간 대응: 계획 퇴근(regOut) 기준으로 결근 판단
            if (pastRegOut) return AttendanceStatus.ABSENT;
            boolean afterLateThreshold = !now.isBefore(regIn.plusMinutes(LATE_MINUTES_THRESHOLD));
            boolean beforeRegOut       = (regOut == null) || now.isBefore(regOut);
            if (afterLateThreshold && beforeRegOut) return AttendanceStatus.LATE; // 예정 출근 지나도 미출근이면 '지각' 상태(프론트에서 빨간 표기 가능)
            if (now.isBefore(regIn)) return AttendanceStatus.PLANNED;
        }

        // 계획조차 없고 날짜가 지나갔다면 결근
        if (pastDay) return AttendanceStatus.ABSENT;

        return AttendanceStatus.PLANNED;
    }

    private static long minutesBetween(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) return 0;
        long m = Duration.between(a, b).toMinutes();
        return Math.max(m, 0);
    }
}
