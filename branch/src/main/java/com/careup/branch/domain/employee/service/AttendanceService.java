package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.dto.request.ScheduleEventUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;
import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final AttendanceStatusResolver statusResolver;

    private static final long CHECKOUT_BLOCK_AFTER_MINUTES = 180;

    public ScheduleEventDetailDto detail(Long scheduleId) {
        var auth = readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        ensureReadable(auth, s);

        ScheduleEvent e = scheduleEventRepository.findByScheduleId(scheduleId).orElse(null);
        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, e, st);
    }

    @Transactional(noRollbackFor = MissedCheckoutLockException.class)
    public ScheduleEventDetailDto upsertEvent(Long scheduleId, ScheduleEventUpdateDto dto) {
        var auth = readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        ensureWritable(auth, s);

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
            LocalDateTime regOut = s.getRegisteredClockOut();
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
            ensureAdmin(auth);
            ev.clearMissedCheckout();
        }

        validateTimeline(ev);

        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now());
        return ScheduleEventDetailDto.of(s, ev, st);
    }

    @Transactional
    public void deleteEvent(Long scheduleId) {
        var auth = readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        ensureWritable(auth, s);

        scheduleEventRepository.findByScheduleId(scheduleId)
                .ifPresent(scheduleEventRepository::delete);
    }

    private void ensureReadable(Auth auth, Schedule s) {
        if (auth.isHqAdmin()) return;

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            LocalDate ref = s.getRegisteredDate() != null ? s.getRegisteredDate() : LocalDate.now();
            List<Branch> myBranches = dispatchStatusRepository
                    .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", ref, ref)
                    .stream().map(DispatchStatus::getBranch).distinct().toList();
            if (myBranches.isEmpty()) throw new AccessDeniedException("권한이 없습니다.");
            boolean ok = myBranches.stream().anyMatch(b -> Objects.equals(b.getId(), s.getBranch().getId()));
            if (!ok) throw new AccessDeniedException("내 지점 소속 데이터만 조회 가능합니다.");
            return;
        }

        if (!Objects.equals(auth.employeeId(), s.getEmployee().getId())) {
            throw new AccessDeniedException("본인 근태만 조회할 수 있습니다.");
        }
    }

    private void ensureWritable(Auth auth, Schedule s) {
        if (auth.isHqAdmin()) return;

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            LocalDate ref = s.getRegisteredDate() != null ? s.getRegisteredDate() : LocalDate.now();
            var myBranches = dispatchStatusRepository
                    .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", ref, ref)
                    .stream().map(DispatchStatus::getBranch).distinct().toList();
            if (myBranches.isEmpty()) throw new AccessDeniedException("권한이 없습니다.");
            boolean ok = myBranches.stream().anyMatch(b -> Objects.equals(b.getId(), s.getBranch().getId()));
            if (!ok) throw new AccessDeniedException("내 지점 소속 데이터만 수정 가능합니다.");
            return;
        }

        if (!Objects.equals(auth.employeeId(), s.getEmployee().getId())) {
            throw new AccessDeniedException("본인 근태만 수정/삭제할 수 있습니다.");
        }
    }

    private void ensureAdmin(Auth auth) {
        if (auth.isHqAdmin() || auth.isBranchOrFranchiseAdmin()) return;
        throw new AccessDeniedException("관리자 권한이 필요합니다.");
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

    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Claims claims)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long employeeId = claims.get("employeeId", Long.class);
        String role = String.valueOf(claims.get("role"));
        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    private record Auth(Long employeeId, String role) {
        public boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        public boolean isBranchOrFranchiseAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
        public Long employeeId() { return employeeId; }
    }
}
