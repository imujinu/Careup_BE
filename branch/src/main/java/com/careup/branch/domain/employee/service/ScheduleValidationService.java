package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.dto.request.ScheduleMassBlockDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.LeaveType;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.entity.WorkType;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleValidationService {

    private final ScheduleRepository scheduleRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final ScheduleTimeService time;

    private static final boolean ALLOW_NIGHT_AFTER_LEAVE = true;
    public static final LocalTime LEAVE_END_CUTOFF = LocalTime.of(18, 0);

    /* ========================
       인증/권한 유틸
       ======================== */
    private record Auth(Long employeeId, String role) {
        boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        boolean isBranchAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
    }

    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Claims c)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long employeeId = c.get("employeeId", Long.class);
        String rawRole = String.valueOf(c.get("role"));
        if (employeeId == null || rawRole == null) throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        String role = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        return new Auth(employeeId, role);
    }

    /**
     * 요청자에게 targetBranchId에 대한 등록 권한이 있는지 확인.
     * HQ_ADMIN 은 전체 허용. BRANCH_ADMIN/FRANCHISE_OWNER 는 자기 소속 지점만 허용.
     */
    public void ensureActorAuthorizedForBranch(Long targetBranchId) {
        var auth = readAuth();
        if (auth.isHqAdmin()) return;

        var me = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다."));

        LocalDate today = LocalDate.now();
        Set<Long> myBranchIds = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        me, "N", today, today)
                .stream()
                .map(ds -> ds.getBranch().getId())
                .collect(Collectors.toSet());

        if (!myBranchIds.contains(targetBranchId)) {
            throw new AccessDeniedException("해당 지점에 대한 등록 권한이 없습니다.");
        }
    }

    /**
     * 직원이 date 일자 기준으로 branchId에 유효 배치인지 확인.
     */
    public void ensureEmployeeAssignableToBranchOnDate(Long employeeId, Long branchId, LocalDate date) {
        boolean ok = dispatchStatusRepository
                .existsByEmployee_IdAndBranch_IdAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employeeId, branchId, "N", date, date);
        if (!ok) {
            Branch b = branchRepository.findById(branchId).orElse(null);
            String bn = (b != null ? b.getName() : String.valueOf(branchId));
            throw new IllegalArgumentException("직원(" + employeeId + ")은 " + date + " 기준 지점(" + bn + ")에 배치되어 있지 않습니다.");
        }
    }

    /* ========================
       개별 저장 시 겹침/중복 검증
       ======================== */
    public void validateNoConflictOnSave(Employee employee,
                                         LocalDate date,
                                         Long branchId,
                                         boolean isLeave,
                                         LocalDateTime in,
                                         LocalDateTime out,
                                         Long selfId) {

        List<Schedule> existed = scheduleRepository.findByEmployeeAndRegisteredDateIn(
                employee, List.of(date.minusDays(1), date, date.plusDays(1))
        );

        ScheduleTimeService.Interval newIv = toInterval(date, isLeave, in, out);

        for (Schedule ex : existed) {
            if (selfId != null && Objects.equals(ex.getId(), selfId)) continue;

            boolean exIsLeave = ex.getCategory() == ScheduleTypeCategory.LEAVE;
            ScheduleTimeService.Interval exIv = toInterval(
                    ex.getRegisteredDate(),
                    exIsLeave,
                    ex.getRegisteredClockIn(),
                    ex.getRegisteredClockOut()
            );

            if (time.isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), branchId)) {
                throw new IllegalStateException("동일 스케줄이 이미 존재합니다.");
            }
            if (time.isOverlap(exIv, newIv)) {
                throw new IllegalStateException("겹치는 스케줄이 존재합니다.");
            }
        }
    }

    /* ========================
       대량 등록 사전 검증
       ======================== */
    public Map<String, Object> massValidate(ScheduleMassCreateDto dto) {
        record Entry(Long employeeId, Long branchId, Long workTypeId, Long leaveTypeId, Long attendanceTemplateId,
                     LocalDate date, LocalTime in, LocalTime bs, LocalTime be, LocalTime out, int index) {}

        List<Entry> entries = new ArrayList<>();
        int idx = 0;

        if (dto.getBlocks() != null) {
            for (ScheduleMassBlockDto b : dto.getBlocks()) {
                for (Long empId : b.getEmployeeIds()) {
                    for (LocalDate d : b.getDates()) {
                        entries.add(new Entry(
                                empId, b.getBranchId(), b.getWorkTypeId(), b.getLeaveTypeId(), b.getAttendanceTemplateId(),
                                d, b.getRegisteredClockInTime(), b.getRegisteredBreakStartTime(),
                                b.getRegisteredBreakEndTime(), b.getRegisteredClockOutTime(), idx++
                        ));
                    }
                }
            }
        }
        if (dto.getItems() != null) {
            for (ScheduleMassItemDto it : dto.getItems()) {
                entries.add(new Entry(
                        it.getEmployeeId(), it.getBranchId(), it.getWorkTypeId(), it.getLeaveTypeId(), it.getAttendanceTemplateId(),
                        it.getDate(), it.getRegisteredClockInTime(), it.getRegisteredBreakStartTime(),
                        it.getRegisteredBreakEndTime(), it.getRegisteredClockOutTime(), idx++
                ));
            }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("검증할 항목이 없습니다.");

        Set<Long> employeeIds = entries.stream().map(Entry::employeeId).collect(Collectors.toSet());
        Set<Long> branchIds   = entries.stream().map(Entry::branchId).collect(Collectors.toSet());
        Set<Long> workIds     = entries.stream().map(Entry::workTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> leaveIds    = entries.stream().map(Entry::leaveTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> tmplIds     = entries.stream().map(Entry::attendanceTemplateId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<LocalDate> dates  = entries.stream().map(Entry::date).collect(Collectors.toSet());

        Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIds)
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, Branch> branchMap = branchRepository.findAllById(branchIds)
                .stream().collect(Collectors.toMap(Branch::getId, Function.identity()));
        Map<Long, WorkType> workMap = workTypeRepository.findAllById(workIds)
                .stream().collect(Collectors.toMap(WorkType::getId, Function.identity()));
        Map<Long, LeaveType> leaveMap = leaveTypeRepository.findAllById(leaveIds)
                .stream().collect(Collectors.toMap(LeaveType::getId, Function.identity()));
        Map<Long, AttendanceTemplate> tmplMap = attendanceTemplateRepository.findAllById(tmplIds)
                .stream().collect(Collectors.toMap(AttendanceTemplate::getId, Function.identity()));

        List<Map<String, Object>> errors = new ArrayList<>();

        for (Entry e : entries) {
            if (!employeeMap.containsKey(e.employeeId())) errors.add(err(e.index(), "INVALID_EMPLOYEE"));
            if (!branchMap.containsKey(e.branchId())) errors.add(err(e.index(), "INVALID_BRANCH"));

            boolean isWork = e.workTypeId() != null;
            boolean isLeave = e.leaveTypeId() != null;
            if (isWork == isLeave) errors.add(err(e.index(), "REQUIRED_EXACTLY_ONE_OF_WORK_OR_LEAVE"));

            if (isWork && !workMap.containsKey(e.workTypeId())) errors.add(err(e.index(), "INVALID_WORK_TYPE"));
            if (isLeave && !leaveMap.containsKey(e.leaveTypeId())) errors.add(err(e.index(), "INVALID_LEAVE_TYPE"));
            if (e.attendanceTemplateId() != null && !tmplMap.containsKey(e.attendanceTemplateId()))
                errors.add(err(e.index(), "INVALID_TEMPLATE"));
        }
        if (!errors.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("valid", false);
            res.put("errors", errors);
            return res;
        }

        Set<LocalDate> datesWithNeighbors = new HashSet<>(dates);
        dates.forEach(d -> {
            datesWithNeighbors.add(d.minusDays(1));
            datesWithNeighbors.add(d.plusDays(1));
        });

        List<Employee> employees = new ArrayList<>(employeeMap.values());
        Map<Long, List<Schedule>> existedByEmp =
                scheduleRepository.findByEmployeeInAndRegisteredDateIn(employees, datesWithNeighbors)
                        .stream().collect(Collectors.groupingBy(s -> s.getEmployee().getId()));

        Map<Long, List<TempSlot>> tempSlotsPerEmp = new HashMap<>();

        for (Entry e : entries) {
            boolean isLeave = (e.leaveTypeId() != null);

            LocalDateTime in;
            LocalDateTime out;

            if (isLeave) {
                in  = e.date().atStartOfDay();
                out = e.date().atTime(LEAVE_END_CUTOFF);
            } else {
                LocalTime tmplIn  = null;
                LocalTime tmplOut = null;
                if (e.attendanceTemplateId() != null && tmplMap.containsKey(e.attendanceTemplateId())) {
                    tmplIn  = tmplMap.get(e.attendanceTemplateId()).getDefaultClockIn();
                    tmplOut = tmplMap.get(e.attendanceTemplateId()).getDefaultClockOut();
                }
                in  = time.coalesce(e.date(), e.in(),  tmplIn);
                out = time.coalesce(e.date(), e.out(), tmplOut);
                if (in == null && out == null) {
                    var span = time.daySpan(e.date());
                    in = span.start();
                    out = span.end();
                }
            }

            ScheduleTimeService.Interval newIv = time.interval(in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(e.employeeId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                boolean exIsLeave = ex.getCategory() == ScheduleTypeCategory.LEAVE;
                ScheduleTimeService.Interval exIv = toInterval(
                        ex.getRegisteredDate(),
                        exIsLeave,
                        ex.getRegisteredClockIn(),
                        ex.getRegisteredClockOut()
                );
                if (time.isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), e.branchId())) {
                    errors.add(err(e.index(), "DUPLICATE_EXISTING"));
                    break;
                }
                if (time.isOverlap(exIv, newIv)) {
                    errors.add(err(e.index(), "OVERLAP_EXISTING"));
                    break;
                }
            }

            List<TempSlot> empSlots = tempSlotsPerEmp.computeIfAbsent(e.employeeId(), k -> new ArrayList<>());
            boolean conflict = false;
            for (TempSlot ts : empSlots) {
                if (time.isExactlySame(ts.interval(), newIv) && Objects.equals(ts.branchId(), e.branchId())) {
                    errors.add(err(e.index(), "DUPLICATE_REQUEST"));
                    conflict = true;
                    break;
                }
                if (time.isOverlap(ts.interval(), newIv)) {
                    errors.add(err(e.index(), "OVERLAP_REQUEST"));
                    conflict = true;
                    break;
                }
            }
            if (!conflict) empSlots.add(new TempSlot(e.branchId(), newIv));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("valid", errors.isEmpty());
        res.put("errors", errors);
        return res;
    }

    private ScheduleTimeService.Interval toInterval(LocalDate baseDate,
                                                    boolean isLeave,
                                                    LocalDateTime in,
                                                    LocalDateTime out) {
        if (isLeave) {
            if (ALLOW_NIGHT_AFTER_LEAVE) {
                return time.interval(baseDate.atStartOfDay(), baseDate.atTime(LEAVE_END_CUTOFF));
            } else {
                return time.daySpan(baseDate);
            }
        }
        if (in == null && out == null) return time.daySpan(baseDate);
        if (in == null || out == null) throw new IllegalArgumentException("근무 스케줄은 출근과 퇴근 시간이 모두 필요합니다.");
        return time.interval(in, out);
    }

    private Map<String, Object> err(int index, String code) {
        Map<String, Object> m = new HashMap<>();
        m.put("index", index);
        m.put("code", code);
        return m;
    }

    private static class TempSlot {
        private final Long branchId;
        private final ScheduleTimeService.Interval interval;
        private TempSlot(Long branchId, ScheduleTimeService.Interval interval) {
            this.branchId = branchId;
            this.interval = interval;
        }
        public Long branchId() { return branchId; }
        public ScheduleTimeService.Interval interval() { return interval; }
    }
}
