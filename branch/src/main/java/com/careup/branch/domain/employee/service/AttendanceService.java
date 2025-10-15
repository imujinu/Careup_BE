package com.careup.branch.domain.employee.service;

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

    private static final long CHECKOUT_BLOCK_AFTER_MINUTES = 180;

    public ScheduleEventDetailDto detail(Long scheduleId) {
        var auth = authz.readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        // 읽기 권한: HQ 전체 / 지점-가맹(해당일자 내 지점 소속 직원) / 직원 본인
        authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent e = scheduleEventRepository.findByScheduleId(scheduleId).orElse(null);
        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, e, st);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto upsertEvent(Long scheduleId, ScheduleEventUpdateDto dto) {
        try {
            var auth = authz.readAuth();

            Schedule s = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

            // 쓰기 권한: HQ / 지점-가맹(내 지점 & 해당일자 대상 직원 배치 확인) / 직원 본인
            authz.ensurePermissionForWrite(auth, s.getBranch(), s.getEmployee(), s.getRegisteredDate());

            ScheduleEvent ev = scheduleEventRepository.findByScheduleId(scheduleId)
                    .orElseGet(() -> scheduleEventRepository.save(
                            ScheduleEvent.builder()
                                    .schedule(s)
                                    .eventDate(dto.getEventDate() != null ? dto.getEventDate() : s.getRegisteredDate())
                                    .missedCheckout(false)
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

            AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
            return ScheduleEventDetailDto.of(s, ev, st);

        } catch (MissedCheckoutLockException ex) {
            // 이벤트는 '퇴근 누락'으로 저장되었고 트랜잭션 롤백 안 함 → 최신 상태를 첨부해서 다시 던지기
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

    private void validateTimeline(ScheduleEvent e) {
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
}
