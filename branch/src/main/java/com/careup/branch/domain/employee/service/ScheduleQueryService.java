package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.dto.response.ScheduleCalendarDto;
import com.careup.branch.domain.employee.dto.response.ScheduleDetailDto;
import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    private final ScheduleAuthService authz;
    private final ScheduleTimeService time;
    private final AttendanceStatusResolver statusResolver;
    private final AttendanceBadgeResolver badgeResolver;

    private final Clock clock;

    public ScheduleDetailDto detail(ScheduleAuthService.Auth auth, Long scheduleId) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());
        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(s.getId()).orElse(null);
        AttendanceStatus st = statusResolver.resolve(s, ev, LocalDateTime.now(clock));
        String badgeText = badgeResolver.toBadgeText(s, st);
        return ScheduleDetailDto.from(s, ev, st, badgeText);
    }

    public List<ScheduleListDto> listAll(ScheduleAuthService.Auth auth, LocalDate from, LocalDate to) {
        LocalDate widenedFrom = from.minusDays(1);
        LocalDate widenedTo = to.plusDays(1);

        if (auth.isHqAdmin()) {
            List<Schedule> candidates = scheduleRepository.findByRegisteredDateBetween(widenedFrom, widenedTo);
            Map<Long, ScheduleEvent> evMap = toEventMap(candidates);
            return candidates.stream()
                    .filter(s -> overlapsRange(s, evMap.get(s.getId()), from, to))
                    .sorted(Comparator.comparing(Schedule::getId))
                    .map(s -> toListDto(s, evMap.get(s.getId())))
                    .toList();
        }

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            List<LocalDate> days = time.datesBetween(from, to);
            Map<LocalDate, Set<Long>> myBranchIdsByDay = new HashMap<>();
            Set<Long> unionBranchIds = new HashSet<>();

            for (LocalDate d : days) {
                Set<Long> branchIds = dispatchStatusRepository
                        .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", d, d)
                        .stream().map(DispatchStatus::getBranch).map(Branch::getId).collect(Collectors.toSet());
                if (!branchIds.isEmpty()) {
                    myBranchIdsByDay.put(d, branchIds);
                    unionBranchIds.addAll(branchIds);
                }
            }
            if (unionBranchIds.isEmpty()) return List.of();

            List<Schedule> candidates = scheduleRepository.findByRegisteredDateBetween(widenedFrom, widenedTo);
            Map<Long, ScheduleEvent> evMap = toEventMap(candidates);

            return candidates.stream()
                    .filter(s -> {
                        Long sid = s.getBranch() != null ? s.getBranch().getId() : null;
                        if (sid == null || !unionBranchIds.contains(sid)) return false;
                        ScheduleEvent e = evMap.get(s.getId());
                        if (!overlapsRange(s, e, from, to)) return false;
                        for (LocalDate d : days) {
                            if (overlapsDay(s, e, d)) {
                                Set<Long> myIds = myBranchIdsByDay.getOrDefault(d, Set.of());
                                if (myIds.contains(sid)) return true;
                            }
                        }
                        return false;
                    })
                    .sorted(Comparator.comparing(Schedule::getId))
                    .map(s -> toListDto(s, evMap.get(s.getId())))
                    .toList();
        }

        return List.of();
    }

    public List<ScheduleListDto> listMine(ScheduleAuthService.Auth auth, LocalDate from, LocalDate to) {
        LocalDate widenedFrom = from.minusDays(1);
        LocalDate widenedTo = to.plusDays(1);

        Employee self = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

        List<Schedule> candidates = scheduleRepository
                .findByEmployeeAndRegisteredDateBetweenOrderByRegisteredDateAsc(self, widenedFrom, widenedTo);

        Map<Long, ScheduleEvent> evMap = scheduleEventRepository
                .findByScheduleIdIn(candidates.stream().map(Schedule::getId).toList())
                .stream().collect(Collectors.toMap(e -> e.getSchedule().getId(), Function.identity(), (a, b) -> a));

        return candidates.stream()
                .filter(s -> overlapsRange(s, evMap.get(s.getId()), from, to))
                .sorted(Comparator.comparing(Schedule::getId))
                .map(s -> toListDto(s, evMap.get(s.getId())))
                .toList();
    }

    public List<ScheduleCalendarDto> calendar(ScheduleAuthService.Auth auth, Long employeeId, String yearMonth) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        authz.ensurePermissionForRead(auth, employee, monthStart);

        LocalDate widenedFrom = monthStart.minusDays(1);
        LocalDate widenedTo = monthEnd.plusDays(1);

        List<Schedule> schedules = scheduleRepository.findByEmployeeAndRegisteredDateBetween(employee, widenedFrom, widenedTo);

        Map<Long, ScheduleEvent> evMap = scheduleEventRepository
                .findByScheduleIdIn(schedules.stream().map(Schedule::getId).toList())
                .stream()
                .collect(Collectors.toMap(e -> e.getSchedule().getId(), Function.identity(), (a, b) -> a));

        List<ScheduleCalendarDto> result = new ArrayList<>();

        for (Schedule s : schedules) {
            ScheduleEvent e = evMap.get(s.getId());
            boolean isLeave = s.getCategory() == ScheduleTypeCategory.LEAVE;

            String title = isLeave
                    ? (s.getLeaveType() != null ? s.getLeaveType().getName() : "Leave")
                    : (s.getWorkType()  != null ? s.getWorkType().getName()  : "Work");

            if (isLeave) {
                LocalDate leaveDate = s.getRegisteredDate();
                if (!leaveDate.isBefore(monthStart) && !leaveDate.isAfter(monthEnd)) {
                    AttendanceStatus st = AttendanceStatus.LEAVE;
                    String badge = badgeResolver.toBadgeText(s, st);
                    result.add(ScheduleCalendarDto.builder()
                            .id(s.getId())
                            .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                            .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                            .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                            .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                            .date(leaveDate)
                            .title(title)
                            .startAt(null)
                            .endAt(null)
                            .allDay(true)
                            .timeSource("ALL_DAY")
                            .status(st)
                            .badgeText(badge)
                            .missedCheckout(false)
                            .build());
                }
                continue;
            }

            String timeSource;
            LocalDateTime start;
            LocalDateTime end;

            boolean hasIn = e != null && e.getClockInAt() != null;
            boolean hasOut = e != null && e.getClockOutAt() != null;
            start = hasIn ? e.getClockInAt() : s.getRegisteredClockIn();
            end   = hasOut ? e.getClockOutAt() : s.getRegisteredClockOut();
            if (start != null && end != null && !end.isAfter(start)) {
                end = end.plusDays(1);
            }
            timeSource = (hasIn && hasOut) ? "ACTUAL" : (hasIn || hasOut) ? "PARTIAL_ACTUAL" : "REGISTERED";

            LocalDate cell = monthStart;
            while (!cell.isAfter(monthEnd)) {
                if (start != null && end != null) {
                    LocalDateTime cellStart = LocalDateTime.of(cell, LocalTime.MIN);
                    LocalDateTime cellEnd   = LocalDateTime.of(cell.plusDays(1), LocalTime.MIDNIGHT);
                    boolean overlaps = start.isBefore(cellEnd) && cellStart.isBefore(end);
                    if (overlaps) {
                        LocalDateTime clippedStart = start.isBefore(cellStart) ? cellStart : start;
                        LocalDateTime clippedEnd   = end.isAfter(cellEnd) ? cellEnd : end;
                        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now(clock));
                        String badge = badgeResolver.toBadgeText(s, st);
                        boolean missed = e != null && e.isMissedCheckout();
                        result.add(ScheduleCalendarDto.builder()
                                .id(s.getId())
                                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                                .date(cell)
                                .title(title)
                                .startAt(clippedStart)
                                .endAt(clippedEnd)
                                .allDay(false)
                                .timeSource(timeSource)
                                .status(st)
                                .badgeText(badge)
                                .missedCheckout(missed)
                                .build());
                    }
                }
                cell = cell.plusDays(1);
            }
        }

        result.sort(Comparator.comparing(ScheduleCalendarDto::getId)
                .thenComparing(ScheduleCalendarDto::getDate)
                .thenComparing(dto -> Optional.ofNullable(dto.getStartAt()).orElse(LocalDateTime.MIN)));

        return result;
    }

    public List<ScheduleCalendarDto> calendarRange(ScheduleAuthService.Auth auth, List<Long> employeeIdsIn, LocalDate from, LocalDate to) {
        LocalDate widenedFrom = from.minusDays(1);
        LocalDate widenedTo = to.plusDays(1);

        final Set<Long> targetIds = (employeeIdsIn == null || employeeIdsIn.isEmpty())
                ? null
                : employeeIdsIn.stream().filter(Objects::nonNull).collect(Collectors.toSet());

        if (auth.isHqAdmin()) {
            List<Schedule> candidates = scheduleRepository.findByRegisteredDateBetween(widenedFrom, widenedTo);
            if (targetIds != null) {
                candidates = candidates.stream()
                        .filter(s -> s.getEmployee() != null && targetIds.contains(s.getEmployee().getId()))
                        .toList();
            }
            return buildCalendarResult(candidates, from, to);
        }

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            List<LocalDate> days = time.datesBetween(from, to);
            Map<LocalDate, Set<Long>> myBranchIdsByDay = new HashMap<>();
            Set<Long> unionBranchIds = new HashSet<>();

            for (LocalDate d : days) {
                Set<Long> branchIds = dispatchStatusRepository
                        .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", d, d)
                        .stream().map(DispatchStatus::getBranch).map(Branch::getId).collect(Collectors.toSet());
                if (!branchIds.isEmpty()) {
                    myBranchIdsByDay.put(d, branchIds);
                    unionBranchIds.addAll(branchIds);
                }
            }
            if (unionBranchIds.isEmpty()) return List.of();

            List<Schedule> candidates = scheduleRepository.findByRegisteredDateBetween(widenedFrom, widenedTo)
                    .stream()
                    .filter(s -> {
                        Long sid = s.getBranch() != null ? s.getBranch().getId() : null;
                        if (sid == null || !unionBranchIds.contains(sid)) return false;
                        if (targetIds != null && (s.getEmployee() == null || !targetIds.contains(s.getEmployee().getId()))) return false;
                        return true;
                    })
                    .toList();

            Map<Long, ScheduleEvent> evMap = toEventMap(candidates);

            List<Schedule> filtered = candidates.stream()
                    .filter(s -> {
                        ScheduleEvent e = evMap.get(s.getId());
                        if (!overlapsRange(s, e, from, to)) return false;
                        for (LocalDate d : days) {
                            if (overlapsDay(s, e, d)) {
                                Set<Long> myIds = myBranchIdsByDay.getOrDefault(d, Set.of());
                                Long sid = s.getBranch() != null ? s.getBranch().getId() : null;
                                if (sid != null && myIds.contains(sid)) return true;
                            }
                        }
                        return false;
                    })
                    .toList();

            return buildCalendarResultWithEvMap(filtered, from, to, evMap);
        }

        Employee self = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
        List<Schedule> mine = scheduleRepository
                .findByEmployeeAndRegisteredDateBetweenOrderByRegisteredDateAsc(self, widenedFrom, widenedTo);
        return buildCalendarResult(mine, from, to);
    }

    private List<ScheduleCalendarDto> buildCalendarResult(List<Schedule> schedules, LocalDate from, LocalDate to) {
        Map<Long, ScheduleEvent> evMap = toEventMap(schedules);
        return buildCalendarResultWithEvMap(schedules, from, to, evMap);
    }

    private List<ScheduleCalendarDto> buildCalendarResultWithEvMap(List<Schedule> schedules, LocalDate from, LocalDate to, Map<Long, ScheduleEvent> evMap) {
        List<ScheduleCalendarDto> result = new ArrayList<>();
        for (Schedule s : schedules) {
            ScheduleEvent e = evMap.get(s.getId());
            boolean isLeave = s.getCategory() == ScheduleTypeCategory.LEAVE;
            String title = isLeave
                    ? (s.getLeaveType() != null ? s.getLeaveType().getName() : "Leave")
                    : (s.getWorkType() != null ? s.getWorkType().getName() : "Work");

            if (isLeave) {
                LocalDate leaveDate = s.getRegisteredDate();
                if (!leaveDate.isBefore(from) && !leaveDate.isAfter(to)) {
                    AttendanceStatus st = AttendanceStatus.LEAVE;
                    String badge = badgeResolver.toBadgeText(s, st);
                    result.add(ScheduleCalendarDto.builder()
                            .id(s.getId())
                            .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                            .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                            .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                            .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                            .date(leaveDate)
                            .title(title)
                            .startAt(null)
                            .endAt(null)
                            .allDay(true)
                            .timeSource("ALL_DAY")
                            .status(st)
                            .badgeText(badge)
                            .missedCheckout(false)
                            .build());
                }
                continue;
            }

            String timeSource;
            LocalDateTime start = null;
            LocalDateTime end = null;

            boolean hasIn = e != null && e.getClockInAt() != null;
            boolean hasOut = e != null && e.getClockOutAt() != null;
            start = hasIn ? e.getClockInAt() : s.getRegisteredClockIn();
            end   = hasOut ? e.getClockOutAt() : s.getRegisteredClockOut();
            if (start != null && end != null && !end.isAfter(start)) end = end.plusDays(1);
            timeSource = (hasIn && hasOut) ? "ACTUAL" : (hasIn || hasOut) ? "PARTIAL_ACTUAL" : "REGISTERED";

            LocalDate d = from;
            while (!d.isAfter(to)) {
                if (start != null && end != null) {
                    LocalDateTime cellStart = LocalDateTime.of(d, LocalTime.MIN);
                    LocalDateTime cellEnd   = LocalDateTime.of(d.plusDays(1), LocalTime.MIDNIGHT);
                    boolean overlaps = start.isBefore(cellEnd) && cellStart.isBefore(end);
                    if (overlaps) {
                        LocalDateTime clippedStart = start.isBefore(cellStart) ? cellStart : start;
                        LocalDateTime clippedEnd   = end.isAfter(cellEnd) ? cellEnd : end;
                        AttendanceStatus st = statusResolver.resolve(s, e, LocalDateTime.now(clock));
                        String badge = badgeResolver.toBadgeText(s, st);
                        boolean missed = e != null && e.isMissedCheckout();
                        result.add(ScheduleCalendarDto.builder()
                                .id(s.getId())
                                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                                .date(d)
                                .title(title)
                                .startAt(clippedStart)
                                .endAt(clippedEnd)
                                .allDay(false)
                                .timeSource(timeSource)
                                .status(st)
                                .badgeText(badge)
                                .missedCheckout(missed)
                                .build());
                    }
                }
                d = d.plusDays(1);
            }
        }
        result.sort(Comparator.comparing(ScheduleCalendarDto::getId)
                .thenComparing(ScheduleCalendarDto::getDate)
                .thenComparing(dto -> Optional.ofNullable(dto.getStartAt()).orElse(LocalDateTime.MIN)));
        return result;
    }

    private Map<Long, ScheduleEvent> toEventMap(List<Schedule> candidates) {
        if (candidates == null || candidates.isEmpty()) return Map.of();
        return scheduleEventRepository
                .findByScheduleIdIn(candidates.stream().map(Schedule::getId).toList())
                .stream()
                .collect(Collectors.toMap(e -> e.getSchedule().getId(), Function.identity(), (a, b) -> a));
    }

    private boolean overlapsRange(Schedule s, ScheduleEvent e, LocalDate from, LocalDate to) {
        Interval iv = toInterval(s, e);
        LocalDateTime rangeStart = LocalDateTime.of(from, LocalTime.MIN);
        LocalDateTime rangeEnd   = LocalDateTime.of(to.plusDays(1), LocalTime.MIDNIGHT);
        if (iv.allDay) {
            LocalDateTime dStart = LocalDateTime.of(s.getRegisteredDate(), LocalTime.MIN);
            LocalDateTime dEnd   = LocalDateTime.of(s.getRegisteredDate().plusDays(1), LocalTime.MIDNIGHT);
            return dStart.isBefore(rangeEnd) && rangeStart.isBefore(dEnd);
        }
        return iv.start.isBefore(rangeEnd) && rangeStart.isBefore(iv.end);
    }

    private boolean overlapsDay(Schedule s, ScheduleEvent e, LocalDate d) {
        Interval iv = toInterval(s, e);
        LocalDateTime dayStart = LocalDateTime.of(d, LocalTime.MIN);
        LocalDateTime dayEnd   = LocalDateTime.of(d.plusDays(1), LocalTime.MIDNIGHT);
        if (iv.allDay) {
            LocalDateTime dStart = LocalDateTime.of(s.getRegisteredDate(), LocalTime.MIN);
            LocalDateTime dEnd   = LocalDateTime.of(s.getRegisteredDate().plusDays(1), LocalTime.MIDNIGHT);
            return dStart.isBefore(dayEnd) && dayStart.isBefore(dEnd);
        }
        return iv.start.isBefore(dayEnd) && dayStart.isBefore(iv.end);
    }

    private Interval toInterval(Schedule s, ScheduleEvent e) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) return Interval.allDay();
        LocalDateTime in  = e != null && e.getClockInAt()  != null ? e.getClockInAt()  : s.getRegisteredClockIn();
        LocalDateTime out = e != null && e.getClockOutAt() != null ? e.getClockOutAt() : s.getRegisteredClockOut();
        if (in == null && out == null) return Interval.allDay();
        if (in == null || out == null) return Interval.allDay();
        LocalDateTime normOut = out.isAfter(in) ? out : out.plusDays(1);
        return Interval.of(in, normOut);
    }

    private ScheduleListDto toListDto(Schedule s, ScheduleEvent e) {
        Totals t = totalsOf(s, e);
        AttendanceStatus status = statusResolver.resolve(s, e, LocalDateTime.now(clock));
        String badgeText = badgeResolver.toBadgeText(s, status);

        return ScheduleListDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee() != null ? s.getEmployee().getId() : null)
                .employeeName(s.getEmployee() != null ? s.getEmployee().getName() : null)
                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                .category(s.getCategory())
                .workTypeId(s.getWorkType() != null ? s.getWorkType().getId() : null)
                .workTypeName(s.getWorkType() != null ? s.getWorkType().getName() : null)
                .leaveTypeId(s.getLeaveType() != null ? s.getLeaveType().getId() : null)
                .leaveTypeName(s.getLeaveType() != null ? s.getLeaveType().getName() : null)
                .registeredDate(s.getRegisteredDate())
                .registeredClockIn(s.getRegisteredClockIn())
                .registeredBreakStart(s.getRegisteredBreakStart())
                .registeredBreakEnd(s.getRegisteredBreakEnd())
                .registeredClockOut(s.getRegisteredClockOut())
                .actualClockIn(e != null ? e.getClockInAt() : null)
                .actualBreakStart(e != null ? e.getBreakStartAt() : null)
                .actualBreakEnd(e != null ? e.getBreakEndAt() : null)
                .actualClockOut(e != null ? e.getClockOutAt() : null)
                .totalBreakMinutes(Math.max(t.breakMin(), 0))
                .totalWorkMinutes(Math.max(t.workMin(), 0))
                .status(status)
                .badgeText(badgeText)
                .build();
    }

    private Totals totalsOf(Schedule s, ScheduleEvent e) {
        if (s.getCategory() == ScheduleTypeCategory.LEAVE) return new Totals(0, 0);

        LocalDateTime in  = e != null && e.getClockInAt()    != null ? e.getClockInAt()    : s.getRegisteredClockIn();
        LocalDateTime out = e != null && e.getClockOutAt()   != null ? e.getClockOutAt()   : s.getRegisteredClockOut();
        LocalDateTime bs  = e != null && e.getBreakStartAt() != null ? e.getBreakStartAt() : s.getRegisteredBreakStart();
        LocalDateTime be  = e != null && e.getBreakEndAt()   != null ? e.getBreakEndAt()   : s.getRegisteredBreakEnd();

        long breakMin = time.minutes(bs, be);
        long workMin  = Math.max(0, time.minutes(in, out) - breakMin);

        if (e != null) {
            int ew = e.getTotalWorkMinutes();
            int eb = e.getTotalBreakMinutes();
            if (ew > 0 || eb > 0) return new Totals(eb, ew);
        }
        return new Totals(breakMin, workMin);
    }

    private record Totals(long breakMin, long workMin) {}

    private static class Interval {
        final boolean allDay;
        final LocalDateTime start;
        final LocalDateTime end;

        private Interval(boolean allDay, LocalDateTime start, LocalDateTime end) {
            this.allDay = allDay;
            this.start = start;
            this.end = end;
        }
        static Interval allDay() { return new Interval(true, null, null); }
        static Interval of(LocalDateTime s, LocalDateTime e) { return new Interval(false, s, e); }
    }
}
