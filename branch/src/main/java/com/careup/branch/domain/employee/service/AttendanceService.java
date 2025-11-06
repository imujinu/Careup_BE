// src/main/java/com/careup/branch/domain/employee/service/AttendanceService.java
package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.service.ChatUserService;
import com.careup.branch.domain.employee.config.AttendanceWindowProperties;
import com.careup.branch.domain.employee.dto.request.AttendanceActionRequest;
import com.careup.branch.domain.employee.dto.request.ScheduleEventUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;
import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.service.SseAlarmService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final AttendanceStatusResolver statusResolver;
    private final ScheduleAuthService authz;
    private final GeofenceValidator geofenceValidator;
    private final Clock clock;
    private final AttendanceWindowProperties windowProps;
    private final SseAlarmService sseAlarmService;
    private static final long CHECKOUT_BLOCK_AFTER_MINUTES = 180;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleTimeService time;

    public ScheduleEventDetailDto detail(Long scheduleId) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());
        ScheduleEvent e = scheduleEventRepository.findByScheduleId(scheduleId).orElse(null);
        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now(clock));
        return ScheduleEventDetailDto.of(s, e, st);
    }

    @Transactional
    public ScheduleEventDetailDto clockIn(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime ts = resolveActionAt(req);
        Double lat = req != null ? req.getLat() : null;
        Double lng = req != null ? req.getLng() : null;
        Integer acc = req != null ? req.getAccuracyMeters() : null;

        Branch branch = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스케줄입니다."))
                .getBranch();

        // 직원 조회 실패해도 본 액션은 진행되도록 보호
        Optional<Employee> empOpt = tryGetEmployeeOrNull();
        empOpt.ifPresent(emp -> {
            SseNotificationResDto dto = SseNotificationResDto.attendanceCheckIn(emp.getName(), branch.getId());
            sseAlarmService.publishNotification(dto);
        });

        return clockInAt(scheduleId, lat, lng, acc, ts);
    }

    @Transactional
    public ScheduleEventDetailDto breakStart(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime ts = resolveActionAt(req);
        boolean explicit = req != null && req.getAt() != null;
        Double lat = req != null ? req.getLat() : null;
        Double lng = req != null ? req.getLng() : null;
        Integer acc = req != null ? req.getAccuracyMeters() : null;
        return breakStartAt(scheduleId, lat, lng, acc, ts, explicit);
    }

    @Transactional
    public ScheduleEventDetailDto breakEnd(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime ts = resolveActionAt(req);
        boolean explicit = req != null && req.getAt() != null;
        Double lat = req != null ? req.getLat() : null;
        Double lng = req != null ? req.getLng() : null;
        Integer acc = req != null ? req.getAccuracyMeters() : null;
        return breakEndAt(scheduleId, lat, lng, acc, ts, explicit);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto clockOut(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime ts = resolveActionAt(req);
        boolean explicit = req != null && req.getAt() != null;
        Double lat = req != null ? req.getLat() : null;
        Double lng = req != null ? req.getLng() : null;
        Integer acc = req != null ? req.getAccuracyMeters() : null;

        Branch branch = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스케줄입니다."))
                .getBranch();

        // 직원 조회 실패해도 본 액션은 진행되도록 보호
        Optional<Employee> empOpt = tryGetEmployeeOrNull();
        empOpt.ifPresent(emp -> {
            SseNotificationResDto dto = SseNotificationResDto.attendanceCheckOut(emp.getName(), branch.getId());
            sseAlarmService.publishNotification(dto);
        });

        return clockOutAt(scheduleId, lat, lng, acc, ts, explicit);
    }

    @Transactional
    public ScheduleEventDetailDto clockInAt(Long scheduleId, Double lat, Double lng, Integer acc, LocalDateTime actionAt) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());
        ensureNotLeaveCategory(s);
        ensureWithinRegisteredWindow(s, actionAt);
        ensureNoOpenEventInOtherSchedules(s);

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseGet(() -> scheduleEventRepository.save(ScheduleEvent.builder()
                        .schedule(s)
                        .eventDate(s.getRegisteredDate())
                        .missedCheckout(false)
                        .totalBreakMinutes(0)
                        .totalWorkMinutes(0)
                        .build()));

        geofenceValidator.validateIfRequired(s, lat, lng, acc, auth);

        if (ev.getClockOutAt() != null) throw new IllegalStateException("이미 퇴근 처리된 스케줄입니다.");
        if (ev.getClockInAt() != null) throw new IllegalStateException("이미 출근 처리되었습니다.");

        ev.changeClockIn(actionAt);
        if (lat != null && lng != null) ev.setClockInLatLon(toBig(lat), toBig(lng));

        validateTimeline(ev);
        recomputeTotals(ev);
        AttendanceStatus st = persistResolvedStatus(s, ev);
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public ScheduleEventDetailDto breakStartAt(Long scheduleId, Double lat, Double lng, Integer acc, LocalDateTime actionAt, boolean explicit) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());
        ensureNotLeaveCategory(s);
        ensureWithinRegisteredWindow(s, actionAt);
        ensureSegmentRealtimeAllowed(s, Segment.BREAK_START, actionAt, explicit);

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

        geofenceValidator.validateIfRequired(s, lat, lng, acc, auth);

        if (ev.getClockInAt() == null) throw new IllegalStateException("출근 기록이 먼저 필요합니다.");
        if (ev.getBreakStartAt() != null && ev.getBreakEndAt() == null) throw new IllegalStateException("이미 휴게 중입니다.");
        if (ev.getBreakEndAt() != null) throw new IllegalStateException("이미 휴게를 종료했습니다.");

        ev.changeBreakStart(actionAt);
        validateTimeline(ev);
        recomputeTotals(ev);
        AttendanceStatus st = persistResolvedStatus(s, ev);
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public ScheduleEventDetailDto breakEndAt(Long scheduleId, Double lat, Double lng, Integer acc, LocalDateTime actionAt, boolean explicit) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());
        ensureNotLeaveCategory(s);
        ensureWithinRegisteredWindow(s, actionAt);
        ensureSegmentRealtimeAllowed(s, Segment.BREAK_END, actionAt, explicit);

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new IllegalStateException("휴게 시작 기록이 없습니다."));

        geofenceValidator.validateIfRequired(s, lat, lng, acc, auth);

        if (ev.getBreakStartAt() == null) throw new IllegalStateException("휴게 시작 기록이 먼저 필요합니다.");
        if (ev.getBreakEndAt() != null) throw new IllegalStateException("이미 휴게 종료 처리되었습니다.");

        ev.changeBreakEnd(actionAt);
        validateTimeline(ev);
        recomputeTotals(ev);
        AttendanceStatus st = persistResolvedStatus(s, ev);
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto clockOutAt(Long scheduleId, Double lat, Double lng, Integer acc, LocalDateTime actionAt, boolean explicit) {
        try {
            var auth = authz.readAuth();
            Schedule s = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
            authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());
            ensureNotLeaveCategory(s);

            ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                    .orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

            var regOut = s.getRegisteredClockOut();
            boolean isAdmin = auth.isHqAdmin() || auth.isBranchOrFranchiseAdmin();

            if (!isAdmin && regOut != null && actionAt.isAfter(regOut) && ev.getClockOutAt() == null) {
                long diffMin = Duration.between(regOut, actionAt).toMinutes();
                if (diffMin >= CHECKOUT_BLOCK_AFTER_MINUTES) {
                    ev.markMissedCheckout();
                    ev.changeAttendanceStatus(statusResolver.resolve(s, ev, LocalDateTime.now(clock)));
                    scheduleEventRepository.save(ev);
                    ScheduleEventDetailDto latest = detail(scheduleId);
                    throw new MissedCheckoutLockException(
                            "퇴근 예정 시각으로부터 3시간이 경과하여 퇴근 처리가 제한되었습니다. 시각을 직접 지정하여 다시 시도해 주세요.", latest);
                }
            }

            ensureWithinRegisteredWindow(s, actionAt);
            ensureSegmentRealtimeAllowed(s, Segment.CLOCK_OUT, actionAt, explicit);

            geofenceValidator.validateIfRequired(s, lat, lng, acc, auth);

            if (ev.getClockInAt() == null) throw new IllegalStateException("출근 기록 없이 퇴근은 불가합니다.");
            if (ev.getClockOutAt() != null) throw new IllegalStateException("이미 퇴근 처리되었습니다.");

            ev.changeClockOut(actionAt);
            if (lat != null && lng != null) ev.setClockOutLatLon(toBig(lat), toBig(lng));

            validateTimeline(ev);
            recomputeTotals(ev);
            AttendanceStatus st = persistResolvedStatus(s, ev);
            return ScheduleEventDetailDto.of(s, ev, st);
        } catch (MissedCheckoutLockException ex) {
            throw ex;
        }
    }

    @Transactional
    public ScheduleEventDetailDto upsertEvent(Long scheduleId, ScheduleEventUpdateDto dto) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent ev = ensureEvent(s, dto.getEventDate());

        if (dto.getEventDate() != null) ev.changeEventDate(dto.getEventDate());
        if (dto.getClockInAt() != null) ev.changeClockIn(dto.getClockInAt());
        if (dto.getBreakStartAt() != null) ev.changeBreakStart(dto.getBreakStartAt());
        if (dto.getBreakEndAt() != null) ev.changeBreakEnd(dto.getBreakEndAt());
        if (dto.getClockOutAt() != null) ev.changeClockOut(dto.getClockOutAt());
        if (Boolean.TRUE.equals(dto.getClearMissedCheckout())) ev.clearMissedCheckout();

        validateTimeline(ev);
        recomputeTotals(ev);
        AttendanceStatus st = persistResolvedStatus(s, ev);
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public void deleteEvent(Long scheduleId) {
        var auth = authz.readAuth();
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());
        scheduleEventRepository.findByScheduleId(scheduleId).ifPresent(scheduleEventRepository::delete);
    }

    private ScheduleEvent ensureEvent(Schedule s, LocalDate defaultEventDate) {
        return scheduleEventRepository.findByScheduleId(s.getId()).orElseGet(() ->
                scheduleEventRepository.save(ScheduleEvent.builder()
                        .schedule(s)
                        .eventDate(defaultEventDate != null ? defaultEventDate : s.getRegisteredDate())
                        .missedCheckout(false)
                        .totalBreakMinutes(0)
                        .totalWorkMinutes(0)
                        .build())
        );
    }

    private void validateTimeline(ScheduleEvent e) {
        if (e.getBreakStartAt() != null && e.getClockInAt() == null)
            throw new IllegalArgumentException("출근 기록 없이 휴게 시작은 불가합니다.");
        if (e.getBreakEndAt() != null && e.getBreakStartAt() == null)
            throw new IllegalArgumentException("휴게 시작 기록 없이 휴게 종료는 불가합니다.");
        if (e.getClockOutAt() != null && e.getClockInAt() == null)
            throw new IllegalArgumentException("출근 기록 없이 퇴근은 불가합니다.");
        if (e.getClockInAt() != null && e.getBreakStartAt() != null && e.getBreakStartAt().isBefore(e.getClockInAt()))
            throw new IllegalArgumentException("휴게 시작 시각이 출근 시각보다 빠를 수 없습니다.");
        if (e.getBreakStartAt() != null && e.getBreakEndAt() != null && e.getBreakEndAt().isBefore(e.getBreakStartAt()))
            throw new IllegalArgumentException("휴게 종료 시각이 시작 시각보다 빠를 수 없습니다.");
        if (e.getClockInAt() != null && e.getClockOutAt() != null && e.getClockOutAt().isBefore(e.getClockInAt()))
            throw new IllegalArgumentException("퇴근 시각이 출근 시각보다 빠를 수 없습니다.");
    }

    private void recomputeTotals(ScheduleEvent e) {
        int breakMin = 0;
        if (e.getBreakStartAt() != null && e.getBreakEndAt() != null) {
            breakMin = safeMinutesBetween(e.getBreakStartAt(), e.getBreakEndAt());
        }
        e.changeTotalBreakMinutes(breakMin);

        if (e.getClockInAt() != null && e.getClockOutAt() != null) {
            int total = safeMinutesBetween(e.getClockInAt(), e.getClockOutAt()) - breakMin;
            e.changeTotalWorkMinutes(Math.max(total, 0));
        } else {
            e.changeTotalWorkMinutes(0);
        }
    }

    private static int safeMinutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        long mins = java.time.Duration.between(start, end).toMinutes();
        return (int) Math.max(mins, 0);
    }

    private static BigDecimal toBig(Double v) {
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }

    private void ensureNoOpenEventInOtherSchedules(Schedule s) {
        Long empId = s.getEmployee() != null ? s.getEmployee().getId() : null;
        if (empId == null) return;
        boolean existsOpenOther =
                scheduleEventRepository.existsBySchedule_Employee_IdAndSchedule_IdNotAndClockInAtIsNotNullAndClockOutAtIsNull(empId, s.getId());
        if (existsOpenOther) throw new IllegalStateException("이전 근무의 퇴근 후 다음 스케줄에서 출근할 수 있습니다.");
    }

    private void ensureNotLeaveCategory(Schedule s) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) {
            throw new IllegalStateException("휴가 일정에서는 출퇴근 기록이 불가합니다.");
        }
    }

    private void ensureWithinRegisteredWindow(Schedule s, LocalDateTime actionAt) {
        if (!windowProps.isEnforce()) return;
        LocalDateTime min = Stream.of(s.getRegisteredClockIn(), s.getRegisteredBreakStart(), s.getRegisteredBreakEnd(), s.getRegisteredClockOut())
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime max = Stream.of(s.getRegisteredClockIn(), s.getRegisteredBreakStart(), s.getRegisteredBreakEnd(), s.getRegisteredClockOut())
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        if (min == null || max == null) {
            LocalDateTime start = s.getRegisteredDate().atStartOfDay();
            min = start;
            max = start.plusDays(1);
        }
        LocalDateTime allowedStart = min.minusMinutes(Math.max(windowProps.getEarlyMinutes(), 0));
        LocalDateTime allowedEnd = max.plusMinutes(Math.max(windowProps.getLateMinutes(), 0));
        if (actionAt.isBefore(allowedStart) || actionAt.isAfter(allowedEnd)) {
            throw new IllegalStateException("등록된 근무 시간 외에는 기록할 수 없습니다.");
        }
    }

    private LocalDateTime resolveActionAt(AttendanceActionRequest req) {
        if (req != null && req.getAt() != null) return req.getAt();
        return LocalDateTime.now(clock);
    }

    private AttendanceStatus persistResolvedStatus(Schedule s, ScheduleEvent ev) {
        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now(clock));
        ev.changeAttendanceStatus(st);
        scheduleEventRepository.save(ev);
        return st;
    }

    public Employee getOwner() {
        Long branchId = ChatUserService.getBranchIdFromToken();
        List<DispatchStatus> list = dispatchStatusRepository.findAllByBranchId(branchId);
        return list.stream()
                .filter(em -> em.getEmployee().getAuthorityType().equals(AuthorityType.HQ_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.BRANCH_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.FRANCHISE_OWNER))
                .findFirst().orElseThrow(() -> new EntityNotFoundException("관리자가 존재하지 않습니다.")).getEmployee();
    }

    public Employee getEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다."));
    }

    /** 직원 조회 실패 시에도 액션을 계속 진행하기 위한 보조 메서드 */
    private Optional<Employee> tryGetEmployeeOrNull() {
        try {
            return Optional.of(getEmployee());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private enum Segment { BREAK_START, BREAK_END, CLOCK_OUT }

    private void ensureSegmentRealtimeAllowed(Schedule s, Segment seg, LocalDateTime actionAt, boolean explicit) {
        if (explicit) return;

        LocalDateTime regIn = s.getRegisteredClockIn();
        LocalDateTime regOut = time.normalizeOut(regIn, s.getRegisteredClockOut());
        LocalDateTime planBrS = s.getRegisteredBreakStart();
        LocalDateTime planBrE = time.normalizeOut(planBrS, s.getRegisteredBreakEnd());

        int early = Math.max(windowProps.getEarlyMinutes(), 0);
        int late  = Math.max(windowProps.getLateMinutes(), 0);

        if (seg == Segment.BREAK_START) {
            if (planBrS == null || planBrE == null) return;
            LocalDateTime earlyBound = planBrS.minusMinutes(early);
            LocalDateTime lateBound  = planBrE.plusMinutes(late);
            if (actionAt.isBefore(earlyBound) || actionAt.isAfter(lateBound)) {
                throw new IllegalStateException("휴게 시작 자동 입력 가능 시간을 벗어났습니다. 시각을 직접 지정해 주세요.");
            }
            return;
        }

        if (seg == Segment.BREAK_END) {
            if (planBrS == null || planBrE == null) return;
            LocalDateTime earlyBound = planBrS;
            LocalDateTime lateBound  = planBrE.plusMinutes(late);
            if (actionAt.isBefore(earlyBound) || actionAt.isAfter(lateBound)) {
                throw new IllegalStateException("휴게 종료 자동 입력 가능 시간을 벗어났습니다. 시각을 직접 지정해 주세요.");
            }
            return;
        }

        if (seg == Segment.CLOCK_OUT) {
            if (regOut == null) return;
            LocalDateTime lateBound = regOut.plusMinutes(late);
            if (actionAt.isAfter(lateBound)) {
                throw new IllegalStateException("퇴근 자동 입력 가능 시간을 지났습니다. 시각을 직접 지정해 주세요.");
            }
        }
    }
}
