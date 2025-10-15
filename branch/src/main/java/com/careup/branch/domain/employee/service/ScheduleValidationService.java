package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.dto.request.ScheduleMassBlockDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleType;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.ScheduleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleValidationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final ScheduleTimeService time;

    private static final boolean ALLOW_NIGHT_AFTER_LEAVE = true;
    public static final LocalTime LEAVE_END_CUTOFF = LocalTime.of(18, 0);

    public void validateNoConflictOnSave(Employee employee,
                                         LocalDate date,
                                         Long branchId,
                                         ScheduleTypeCategory category,
                                         LocalDateTime in,
                                         LocalDateTime out,
                                         Long selfId) {

        List<Schedule> existed = scheduleRepository.findByEmployeeAndRegisteredDateIn(
                employee, List.of(date.minusDays(1), date, date.plusDays(1))
        );

        ScheduleTimeService.Interval newIv = toInterval(date, category, in, out);

        for (Schedule ex : existed) {
            if (selfId != null && Objects.equals(ex.getId(), selfId)) continue;

            ScheduleTimeService.Interval exIv = toInterval(
                    ex.getRegisteredDate(),
                    ex.getScheduleType().getCategory(),
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

    public Map<String, Object> massValidate(ScheduleMassCreateDto dto) {
        record Entry(Long employeeId, Long branchId, Long scheduleTypeId, Long attendanceTemplateId,
                     LocalDate date, LocalTime in, LocalTime bs, LocalTime be, LocalTime out, int index) {}

        List<Entry> entries = new ArrayList<>();
        int idx = 0;

        if (dto.getBlocks() != null) {
            for (ScheduleMassBlockDto b : dto.getBlocks()) {
                for (Long empId : b.getEmployeeIds()) {
                    for (LocalDate d : b.getDates()) {
                        entries.add(new Entry(
                                empId, b.getBranchId(), b.getScheduleTypeId(), b.getAttendanceTemplateId(),
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
                        it.getEmployeeId(), it.getBranchId(), it.getScheduleTypeId(), it.getAttendanceTemplateId(),
                        it.getDate(), it.getRegisteredClockInTime(), it.getRegisteredBreakStartTime(),
                        it.getRegisteredBreakEndTime(), it.getRegisteredClockOutTime(), idx++
                ));
            }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("검증할 항목이 없습니다.");

        Set<Long> employeeIds = entries.stream().map(Entry::employeeId).collect(Collectors.toSet());
        Set<Long> branchIds   = entries.stream().map(Entry::branchId).collect(Collectors.toSet());
        Set<Long> typeIds     = entries.stream().map(Entry::scheduleTypeId).collect(Collectors.toSet());
        Set<Long> tmplIds     = entries.stream().map(Entry::attendanceTemplateId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<LocalDate> dates  = entries.stream().map(Entry::date).collect(Collectors.toSet());

        Map<Long, Employee> employeeMap = employeeRepository.findAllById(employeeIds)
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, Branch> branchMap = branchRepository.findAllById(branchIds)
                .stream().collect(Collectors.toMap(Branch::getId, Function.identity()));
        Map<Long, ScheduleType> typeMap = scheduleTypeRepository.findAllById(typeIds)
                .stream().collect(Collectors.toMap(ScheduleType::getId, Function.identity()));
        Map<Long, AttendanceTemplate> tmplMap = attendanceTemplateRepository.findAllById(tmplIds)
                .stream().collect(Collectors.toMap(AttendanceTemplate::getId, Function.identity()));

        List<Map<String, Object>> errors = new ArrayList<>();

        for (Entry e : entries) {
            if (!employeeMap.containsKey(e.employeeId())) errors.add(err(e.index(), "INVALID_EMPLOYEE"));
            if (!branchMap.containsKey(e.branchId())) errors.add(err(e.index(), "INVALID_BRANCH"));
            if (!typeMap.containsKey(e.scheduleTypeId())) errors.add(err(e.index(), "INVALID_SCHEDULE_TYPE"));
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
            Employee emp = employeeMap.get(e.employeeId());
            Branch br    = branchMap.get(e.branchId());
            ScheduleType tp = typeMap.get(e.scheduleTypeId());
            AttendanceTemplate tmpl = e.attendanceTemplateId() != null ? tmplMap.get(e.attendanceTemplateId()) : null;

            LocalDateTime in, out;
            if (tp.getCategory() == ScheduleTypeCategory.LEAVE) {
                in = e.date().atStartOfDay();
                out = e.date().atTime(LEAVE_END_CUTOFF);
            } else {
                in  = time.coalesce(e.date(), e.in(),  tmpl != null ? tmpl.getDefaultClockIn()  : null);
                out = time.coalesce(e.date(), e.out(), tmpl != null ? tmpl.getDefaultClockOut() : null);
                if (in == null && out == null) {
                    var span = time.daySpan(e.date());
                    in = span.start();
                    out = span.end();
                }
            }

            ScheduleTimeService.Interval newIv = time.interval(in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(emp.getId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                ScheduleTimeService.Interval exIv = toInterval(
                        ex.getRegisteredDate(),
                        ex.getScheduleType().getCategory(),
                        ex.getRegisteredClockIn(),
                        ex.getRegisteredClockOut()
                );
                if (time.isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), br.getId())) {
                    errors.add(err(e.index(), "DUPLICATE_EXISTING"));
                    break;
                }
                if (time.isOverlap(exIv, newIv)) {
                    errors.add(err(e.index(), "OVERLAP_EXISTING"));
                    break;
                }
            }

            List<TempSlot> empSlots = tempSlotsPerEmp.computeIfAbsent(emp.getId(), k -> new ArrayList<>());
            boolean conflict = false;
            for (TempSlot ts : empSlots) {
                if (time.isExactlySame(ts.interval(), newIv) && Objects.equals(ts.branchId(), br.getId())) {
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
            if (!conflict) empSlots.add(new TempSlot(br.getId(), newIv));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("valid", errors.isEmpty());
        res.put("errors", errors);
        return res;
    }

    private ScheduleTimeService.Interval toInterval(LocalDate baseDate,
                                                    ScheduleTypeCategory category,
                                                    LocalDateTime in,
                                                    LocalDateTime out) {
        if (category == ScheduleTypeCategory.LEAVE) {
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
