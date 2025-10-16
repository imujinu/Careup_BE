package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.AttendanceActionRequest;
import com.careup.branch.domain.employee.dto.request.ScheduleEventUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;
import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final AttendanceStatusResolver statusResolver;
    private final ScheduleAuthService authz;
    private final GeofenceValidator geofenceValidator;

    private static final long CHECKOUT_BLOCK_AFTER_MINUTES = 180;

    public ScheduleEventDetailDto detail(Long scheduleId) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent e = scheduleEventRepository.findByScheduleId(scheduleId).orElse(null);
        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, e, st);
    }

    /* =======================
       Public APIs (실사용: now)
       ======================= */

    @Transactional
    public ScheduleEventDetailDto clockIn(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime now = LocalDateTime.now();
        return clockInAt(scheduleId, req != null ? req.getLat() : null, req != null ? req.getLng() : null, now);
    }

    @Transactional
    public ScheduleEventDetailDto breakStart(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime now = LocalDateTime.now();
        return breakStartAt(scheduleId, req != null ? req.getLat() : null, req != null ? req.getLng() : null, now);
    }

    @Transactional
    public ScheduleEventDetailDto breakEnd(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime now = LocalDateTime.now();
        return breakEndAt(scheduleId, req != null ? req.getLat() : null, req != null ? req.getLng() : null, now);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto clockOut(Long scheduleId, AttendanceActionRequest req) {
        LocalDateTime now = LocalDateTime.now();
        return clockOutAt(scheduleId, req != null ? req.getLat() : null, req != null ? req.getLng() : null, now);
    }

    /* =======================================
       시더/테스트용: 고정 actionAt 지정
       ======================================= */

    @Transactional
    public ScheduleEventDetailDto clockInAt(Long scheduleId, Double lat, Double lng, LocalDateTime actionAt) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseGet(() -> scheduleEventRepository.save(
                        ScheduleEvent.builder()
                                .schedule(s)
                                .eventDate(s.getRegisteredDate())
                                .missedCheckout(false)
                                .totalBreakMinutes(0)
                                .totalWorkMinutes(0)
                                .build()
                ));

        geofenceValidator.validateIfRequired(s, lat, lng);

        if (ev.getClockOutAt() != null) {
            throw new IllegalStateException("이미 퇴근 처리된 스케줄입니다.");
        }

        ev.changeClockIn(actionAt);
        if (lat != null && lng != null) {
            ev.setClockInLatLon(toBig(lat), toBig(lng));
        }

        validateTimeline(ev);
        recomputeTotals(ev); // ★ 합산 업데이트

        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public ScheduleEventDetailDto breakStartAt(Long scheduleId, Double lat, Double lng, LocalDateTime actionAt) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

        geofenceValidator.validateIfRequired(s, lat, lng);

        if (ev.getClockInAt() == null) {
            throw new IllegalStateException("출근 기록이 먼저 필요합니다.");
        }
        if (ev.getBreakStartAt() != null && ev.getBreakEndAt() == null) {
            throw new IllegalStateException("이미 휴게 중입니다.");
        }

        ev.changeBreakStart(actionAt);

        validateTimeline(ev);
        recomputeTotals(ev); // ★ 합산 업데이트

        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public ScheduleEventDetailDto breakEndAt(Long scheduleId, Double lat, Double lng, LocalDateTime actionAt) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new IllegalStateException("휴게 시작 기록이 없습니다."));

        geofenceValidator.validateIfRequired(s, lat, lng);

        if (ev.getBreakStartAt() == null) {
            throw new IllegalStateException("휴게 시작 기록이 먼저 필요합니다.");
        }

        ev.changeBreakEnd(actionAt);

        validateTimeline(ev);
        recomputeTotals(ev); // ★ 합산 업데이트

        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto clockOutAt(Long scheduleId, Double lat, Double lng, LocalDateTime actionAt) {
        try {
            var auth = authz.readAuth();

            Schedule s = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
            authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

            ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                    .orElseThrow(() -> new IllegalStateException("출근 기록이 없습니다."));

            geofenceValidator.validateIfRequired(s, lat, lng);

            if (ev.getClockInAt() == null) {
                throw new IllegalStateException("출근 기록이 먼저 필요합니다.");
            }

            var regOut = s.getRegisteredClockOut();
            boolean isAdmin = auth.isHqAdmin() || auth.isBranchOrFranchiseAdmin();

            if (!isAdmin && regOut != null && actionAt.isAfter(regOut) && ev.getClockOutAt() == null) {
                long diffMin = Duration.between(regOut, actionAt).toMinutes();
                if (diffMin >= CHECKOUT_BLOCK_AFTER_MINUTES) {
                    ev.markMissedCheckout();
                    scheduleEventRepository.save(ev);
                    ScheduleEventDetailDto latest = detail(scheduleId);
                    throw new MissedCheckoutLockException("퇴근 예정 시각으로부터 3시간이 경과하여 퇴근 처리가 제한되었습니다.", latest);
                }
            }

            ev.changeClockOut(actionAt);
            if (lat != null && lng != null) {
                ev.setClockOutLatLon(toBig(lat), toBig(lng));
            }

            validateTimeline(ev);
            recomputeTotals(ev); // ★ 합산 업데이트

            AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
            return ScheduleEventDetailDto.of(s, ev, st);

        } catch (MissedCheckoutLockException ex) {
            throw ex;
        }
    }

    /* ===== 기존 PATCH (관리자/보정용) ===== */

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto upsertEvent(Long scheduleId, ScheduleEventUpdateDto dto) {
        try {
            var auth = authz.readAuth();

            Schedule s = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
            authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

            ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                    .orElseGet(() -> scheduleEventRepository.save(
                            ScheduleEvent.builder()
                                    .schedule(s)
                                    .eventDate(dto.getEventDate() != null ? dto.getEventDate() : s.getRegisteredDate())
                                    .missedCheckout(false)
                                    .totalBreakMinutes(0)
                                    .totalWorkMinutes(0)
                                    .build()
                    ));

            if (dto.getEventDate() != null)    ev.changeEventDate(dto.getEventDate());
            if (dto.getClockInAt() != null)    ev.changeClockIn(dto.getClockInAt());
            if (dto.getBreakStartAt() != null) ev.changeBreakStart(dto.getBreakStartAt());
            if (dto.getBreakEndAt() != null)   ev.changeBreakEnd(dto.getBreakEndAt());

            if (dto.getClockOutAt() != null) {
                LocalDateTime now = LocalDateTime.now();
                var regOut = s.getRegisteredClockOut();
                boolean isAdmin = auth.isHqAdmin() || auth.isBranchOrFranchiseAdmin();

                if (!isAdmin) {
                    if (regOut != null && now.isAfter(regOut)) {
                        long diffMin = Duration.between(regOut, now).toMinutes();
                        if (diffMin >= CHECKOUT_BLOCK_AFTER_MINUTES && ev.getClockOutAt() == null) {
                            ev.markMissedCheckout();
                            scheduleEventRepository.save(ev);
                            throw new MissedCheckoutLockException("퇴근 예정 시각으로부터 3시간이 경과하여 퇴근 처리가 불가능합니다. (퇴근 누락으로 기록)");
                        }
                    }
                } else {
                    if (Boolean.TRUE.equals(dto.getClearMissedCheckout())) {
                        ev.clearMissedCheckout();
                    }
                }
                ev.changeClockOut(dto.getClockOutAt());
            } else if (Boolean.TRUE.equals(dto.getClearMissedCheckout())) {
                if (!(auth.isHqAdmin() || auth.isBranchOrFranchiseAdmin())) {
                    throw new AccessDeniedException("관리자 권한이 필요합니다.");
                }
                ev.clearMissedCheckout();
            }

            validateTimeline(ev);
            recomputeTotals(ev); // ★ 합산 업데이트

            AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
            return ScheduleEventDetailDto.of(s, ev, st);

        } catch (MissedCheckoutLockException ex) {
            ScheduleEventDetailDto latest = detail(scheduleId);
            throw new MissedCheckoutLockException(ex.getMessage(), latest);
        }
    }

    @Transactional
    public void deleteEvent(Long scheduleId) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

        scheduleEventRepository.findByScheduleId(scheduleId)
                .ifPresent(scheduleEventRepository::delete);
    }

    /* ===== 타임라인/순서 검증 ===== */

    private void validateTimeline(ScheduleEvent e) {
        if (e.getBreakStartAt() != null && e.getClockInAt() == null) {
            throw new IllegalArgumentException("출근 기록 없이 휴게 시작은 불가합니다.");
        }
        if (e.getBreakEndAt() != null && e.getBreakStartAt() == null) {
            throw new IllegalArgumentException("휴게 시작 기록 없이 휴게 종료는 불가합니다.");
        }
        if (e.getClockOutAt() != null && e.getClockInAt() == null) {
            throw new IllegalArgumentException("출근 기록 없이 퇴근은 불가합니다.");
        }

        if (e.getClockInAt() != null && e.getBreakStartAt() != null) {
            if (e.getBreakStartAt().isBefore(e.getClockInAt())) {
                throw new IllegalArgumentException("휴게 시작 시각이 출근 시각보다 빠를 수 없습니다.");
            }
        }
        if (e.getBreakStartAt() != null && e.getBreakEndAt() != null) {
            if (e.getBreakEndAt().isBefore(e.getBreakStartAt())) {
                throw new IllegalArgumentException("휴게 종료 시각이 시작 시각보다 빠를 수 없습니다.");
            }
        }
        if (e.getClockInAt() != null && e.getClockOutAt() != null) {
            if (e.getClockOutAt().isBefore(e.getClockInAt())) {
                throw new IllegalArgumentException("퇴근 시각이 출근 시각보다 빠를 수 없습니다.");
            }
        }
    }

    /* ===== 합산 로직 (여기가 핵심) ===== */

    private void recomputeTotals(ScheduleEvent e) {
        // 휴게 시간 계산 (단일 구간)
        int breakMin = 0;
        if (e.getBreakStartAt() != null && e.getBreakEndAt() != null) {
            breakMin = safeMinutesBetween(e.getBreakStartAt(), e.getBreakEndAt());
        }
        e.changeTotalBreakMinutes(breakMin);

        // 실제 근무 시간 = (출근~퇴근) - 휴게
        if (e.getClockInAt() != null && e.getClockOutAt() != null) {
            int total = safeMinutesBetween(e.getClockInAt(), e.getClockOutAt()) - breakMin;
            e.changeTotalWorkMinutes(Math.max(total, 0));
        } else {
            // 퇴근 전에는 확정 근무분을 0으로 유지(원하면 임시 합계로 업데이트할 수도 있음)
            e.changeTotalWorkMinutes(0);
        }
    }

    private static int safeMinutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        long mins = Duration.between(start, end).toMinutes();
        return (int) Math.max(mins, 0);
    }

    private static BigDecimal toBig(Double v) {
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }
}
