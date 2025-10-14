package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.dto.request.ScheduleCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassBlockDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto;
import com.careup.branch.domain.employee.dto.request.ScheduleUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleCalendarDto;
import com.careup.branch.domain.employee.dto.response.ScheduleDetailDto;
import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleType;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.ScheduleTypeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    @Transactional
    public ScheduleDetailDto create(ScheduleCreateDto dto) {
        var auth = readAuth();

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));
        ScheduleType type = scheduleTypeRepository.findById(dto.getScheduleTypeId())
                .orElseThrow(() -> new EntityNotFoundException("스케줄 종류를 찾을 수 없습니다."));
        AttendanceTemplate template = null;
        if (dto.getAttendanceTemplateId() != null) {
            template = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("템플릿을 찾을 수 없습니다."));
        }
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());

        LocalDate date = dto.getRegisteredDate();
        LocalDateTime in  = coalesceDateTime(dto.getRegisteredClockIn(),  date, template != null ? template.getDefaultClockIn()  : null);
        LocalDateTime bs  = coalesceDateTime(dto.getRegisteredBreakStart(), date, template != null ? template.getDefaultBreakStart() : null);
        LocalDateTime be  = coalesceDateTime(dto.getRegisteredBreakEnd(), date, template != null ? template.getDefaultBreakEnd() : null);
        LocalDateTime out = coalesceDateTime(dto.getRegisteredClockOut(), date, template != null ? template.getDefaultClockOut() : null);

        if (dto.getRegisteredClockIn() != null && dto.getRegisteredClockOut() != null) {
            if (out == null || in == null || !out.isAfter(in)) {
                throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후여야 합니다.");
            }
        }

        validateNoConflictOnSave(employee, date, branch.getId(), type.getCategory(), in, out, null);

        Schedule saved = scheduleRepository.save(
                Schedule.builder()
                        .branch(branch)
                        .employee(employee)
                        .scheduleType(type)
                        .attendanceTemplate(template)
                        .registeredDate(date)
                        .registeredClockIn(in)
                        .registeredBreakStart(bs)
                        .registeredBreakEnd(be)
                        .registeredClockOut(out)
                        .build()
        );

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(saved.getId()).orElse(null);
        return ScheduleDetailDto.from(saved, ev);
    }

    @Transactional
    public List<ScheduleDetailDto> massCreate(ScheduleMassCreateDto dto) {
        var auth = readAuth();

        record Entry(Long employeeId, Long branchId, Long scheduleTypeId, Long attendanceTemplateId,
                     LocalDate date, LocalTime in, LocalTime bs, LocalTime be, LocalTime out) {}

        List<Entry> entries = new ArrayList<>();

        if (dto.getBlocks() != null) {
            for (ScheduleMassBlockDto b : dto.getBlocks()) {
                for (Long empId : b.getEmployeeIds()) {
                    for (LocalDate d : b.getDates()) {
                        entries.add(new Entry(
                                empId, b.getBranchId(), b.getScheduleTypeId(), b.getAttendanceTemplateId(),
                                d, b.getRegisteredClockInTime(), b.getRegisteredBreakStartTime(),
                                b.getRegisteredBreakEndTime(), b.getRegisteredClockOutTime()
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
                        it.getRegisteredBreakEndTime(), it.getRegisteredClockOutTime()
                ));
            }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("등록할 항목이 없습니다.");

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

        for (Entry e : entries) {
            if (!employeeMap.containsKey(e.employeeId())) throw new EntityNotFoundException("직원 없음: " + e.employeeId());
            if (!branchMap.containsKey(e.branchId())) throw new EntityNotFoundException("지점 없음: " + e.branchId());
            if (!typeMap.containsKey(e.scheduleTypeId())) throw new EntityNotFoundException("스케줄 종류 없음: " + e.scheduleTypeId());
            if (e.attendanceTemplateId() != null && !tmplMap.containsKey(e.attendanceTemplateId()))
                throw new EntityNotFoundException("템플릿 없음: " + e.attendanceTemplateId());
        }
        for (Entry e : entries) {
            ensurePermissionForWrite(auth, branchMap.get(e.branchId()), employeeMap.get(e.employeeId()), e.date());
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
        List<Schedule> toSave = new ArrayList<>();

        for (Entry e : entries) {
            Employee emp = employeeMap.get(e.employeeId());
            Branch br    = branchMap.get(e.branchId());
            ScheduleType tp = typeMap.get(e.scheduleTypeId());
            AttendanceTemplate tmpl = e.attendanceTemplateId() != null ? tmplMap.get(e.attendanceTemplateId()) : null;

            LocalDateTime in  = coalesce(e.date(), e.in(),  tmpl != null ? tmpl.getDefaultClockIn()  : null);
            LocalDateTime bs  = coalesce(e.date(), e.bs(),  tmpl != null ? tmpl.getDefaultBreakStart() : null);
            LocalDateTime be  = coalesce(e.date(), e.be(),  tmpl != null ? tmpl.getDefaultBreakEnd()   : null);
            LocalDateTime out = coalesce(e.date(), e.out(), tmpl != null ? tmpl.getDefaultClockOut() : null);

            Interval newIv = toInterval(tp.getCategory(), in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(emp.getId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                Interval exIv = toInterval(ex.getScheduleType().getCategory(), ex.getRegisteredClockIn(), ex.getRegisteredClockOut());
                if (isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), br.getId())) {
                    throw new IllegalStateException("동일 스케줄이 이미 존재합니다.");
                }
                if (isOverlap(exIv, newIv)) {
                    throw new IllegalStateException("겹치는 스케줄이 존재합니다.");
                }
            }

            List<TempSlot> empSlots = tempSlotsPerEmp.computeIfAbsent(emp.getId(), k -> new ArrayList<>());
            for (TempSlot ts : empSlots) {
                if (isExactlySame(ts.interval(), newIv) && Objects.equals(ts.branchId(), br.getId())) {
                    throw new IllegalStateException("요청 내 동일 스케줄이 중복되었습니다.");
                }
                if (isOverlap(ts.interval(), newIv)) {
                    throw new IllegalStateException("요청 내 시간대가 서로 겹칩니다.");
                }
            }
            empSlots.add(new TempSlot(br.getId(), newIv));

            toSave.add(
                    Schedule.builder()
                            .branch(br)
                            .employee(emp)
                            .scheduleType(tp)
                            .attendanceTemplate(tmpl)
                            .registeredDate(e.date())
                            .registeredClockIn(in)
                            .registeredBreakStart(bs)
                            .registeredBreakEnd(be)
                            .registeredClockOut(out)
                            .build()
            );
        }

        List<Schedule> saved = scheduleRepository.saveAll(toSave);
        return saved.stream()
                .sorted(Comparator.comparing(Schedule::getRegisteredDate).thenComparing(s -> s.getEmployee().getId()))
                .map(s -> ScheduleDetailDto.from(s, (ScheduleEvent) null))
                .toList();
    }

    public Map<String, Object> massValidate(ScheduleMassCreateDto dto) {
        var auth = readAuth();

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
        for (Entry e : entries) {
            if (!employeeMap.containsKey(e.employeeId()) || !branchMap.containsKey(e.branchId()) || !typeMap.containsKey(e.scheduleTypeId())) continue;
            try {
                ensurePermissionForWrite(auth, branchMap.get(e.branchId()), employeeMap.get(e.employeeId()), e.date());
            } catch (RuntimeException ex) {
                errors.add(err(e.index(), "NO_PERMISSION"));
            }
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
            if (!employeeMap.containsKey(e.employeeId()) || !branchMap.containsKey(e.branchId()) || !typeMap.containsKey(e.scheduleTypeId())) continue;

            Employee emp = employeeMap.get(e.employeeId());
            Branch br    = branchMap.get(e.branchId());
            ScheduleType tp = typeMap.get(e.scheduleTypeId());
            AttendanceTemplate tmpl = e.attendanceTemplateId() != null ? tmplMap.get(e.attendanceTemplateId()) : null;

            LocalDateTime in  = coalesce(e.date(), e.in(),  tmpl != null ? tmpl.getDefaultClockIn()  : null);
            LocalDateTime out = coalesce(e.date(), e.out(), tmpl != null ? tmpl.getDefaultClockOut() : null);

            Interval newIv = toInterval(tp.getCategory(), in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(emp.getId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                Interval exIv = toInterval(ex.getScheduleType().getCategory(), ex.getRegisteredClockIn(), ex.getRegisteredClockOut());
                if (isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), br.getId())) {
                    errors.add(err(e.index(), "DUPLICATE_EXISTING"));
                    break;
                }
                if (isOverlap(exIv, newIv)) {
                    errors.add(err(e.index(), "OVERLAP_EXISTING"));
                    break;
                }
            }

            List<TempSlot> empSlots = tempSlotsPerEmp.computeIfAbsent(emp.getId(), k -> new ArrayList<>());
            boolean conflict = false;
            for (TempSlot ts : empSlots) {
                if (isExactlySame(ts.interval(), newIv) && Objects.equals(ts.branchId(), br.getId())) {
                    errors.add(err(e.index(), "DUPLICATE_REQUEST"));
                    conflict = true;
                    break;
                }
                if (isOverlap(ts.interval(), newIv)) {
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

    @Transactional
    public ScheduleDetailDto update(Long scheduleId, ScheduleUpdateDto dto) {
        var auth = readAuth();

        Schedule target = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));
        ensureUpdatable(scheduleId);

        Employee employee = target.getEmployee();
        ScheduleType type = scheduleTypeRepository.findById(dto.getScheduleTypeId())
                .orElseThrow(() -> new EntityNotFoundException("스케줄 종류를 찾을 수 없습니다."));
        AttendanceTemplate template = null;
        if (dto.getAttendanceTemplateId() != null) {
            template = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("템플릿을 찾을 수 없습니다."));
        }
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());

        LocalDate date = dto.getRegisteredDate();
        LocalDateTime in  = coalesceDateTime(dto.getRegisteredClockIn(),  date, template != null ? template.getDefaultClockIn()  : null);
        LocalDateTime bs  = coalesceDateTime(dto.getRegisteredBreakStart(), date, template != null ? template.getDefaultBreakStart() : null);
        LocalDateTime be  = coalesceDateTime(dto.getRegisteredBreakEnd(), date, template != null ? template.getDefaultBreakEnd() : null);
        LocalDateTime out = coalesceDateTime(dto.getRegisteredClockOut(), date, template != null ? template.getDefaultClockOut() : null);

        if (dto.getRegisteredClockIn() != null && dto.getRegisteredClockOut() != null) {
            if (out == null || in == null || !out.isAfter(in)) {
                throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후여야 합니다.");
            }
        }

        validateNoConflictOnSave(employee, date, branch.getId(), type.getCategory(), in, out, target.getId());

        target.change(type, template, date, in, bs, be, out);
        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(target.getId()).orElse(null);
        return ScheduleDetailDto.from(target, ev);
    }

    @Transactional
    public void delete(Long scheduleId) {
        var auth = readAuth();

        Schedule target = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        ensurePermissionForRead(auth, target.getEmployee(), target.getRegisteredDate());

        boolean hasEvent = scheduleEventRepository.findByScheduleId(scheduleId).isPresent();
        if (hasEvent) {
            throw new IllegalStateException("이미 근태 이벤트가 존재합니다. 이벤트 삭제 후 스케줄을 삭제하세요.");
        }
        scheduleRepository.delete(target);
    }

    @Transactional
    public void deleteMany(List<Long> scheduleIds) {
        var auth = readAuth();

        if (scheduleIds == null || scheduleIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 스케줄이 없습니다.");
        }

        List<Schedule> targets = scheduleRepository.findAllById(scheduleIds);
        if (targets.size() != new HashSet<>(scheduleIds).size()) {
            throw new EntityNotFoundException("일부 스케줄을 찾을 수 없습니다.");
        }

        for (Schedule s : targets) {
            ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());
        }

        long evCount = scheduleEventRepository.countByScheduleIdIn(scheduleIds);
        if (evCount > 0) {
            throw new IllegalStateException("일부 스케줄에 근태 이벤트가 존재합니다. 이벤트 삭제 후 다시 시도하세요.");
        }

        scheduleRepository.deleteAllByIdInBatch(scheduleIds);
    }

    public ScheduleDetailDto detail(Long scheduleId) {
        var auth = readAuth();

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());

        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(s.getId()).orElse(null);
        return ScheduleDetailDto.from(s, ev);
    }

    public List<ScheduleListDto> list(Long employeeId, LocalDate from, LocalDate to) {
        var auth = readAuth();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        ensurePermissionForRead(auth, employee, from);

        List<Schedule> schedules = scheduleRepository
                .findByEmployeeAndRegisteredDateBetweenOrderByRegisteredDateAsc(employee, from, to);

        Map<Long, ScheduleEvent> evMap = scheduleEventRepository
                .findByScheduleIdIn(schedules.stream().map(Schedule::getId).toList())
                .stream()
                .collect(Collectors.toMap(e -> e.getSchedule().getId(), Function.identity(), (a, b) -> a));

        return schedules.stream()
                .map(s -> ScheduleListDto.from(s, evMap.get(s.getId())))
                .toList();
    }

    public List<ScheduleCalendarDto> calendar(Long employeeId, String yearMonth) {
        var auth = readAuth();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        ensurePermissionForRead(auth, employee, from);

        List<Schedule> schedules = scheduleRepository.findByEmployeeAndRegisteredDateBetween(employee, from, to);

        Map<Long, ScheduleEvent> evMap = scheduleEventRepository
                .findByScheduleIdIn(schedules.stream().map(Schedule::getId).toList())
                .stream()
                .collect(Collectors.toMap(e -> e.getSchedule().getId(), Function.identity(), (a, b) -> a));

        return schedules.stream()
                .map(s -> ScheduleCalendarDto.fromResolved(s, evMap.get(s.getId())))
                .toList();
    }

    private void ensureUpdatable(Long scheduleId) {
        boolean hasEvent = scheduleEventRepository.findByScheduleId(scheduleId).isPresent();
        if (hasEvent) throw new IllegalStateException("이미 근태 이벤트가 존재합니다. 이벤트 삭제 후 수정하세요.");
    }

    private void ensurePermissionForWrite(Auth auth, Branch branch, Employee targetEmployee, LocalDate onDate) {
        if (auth.isHqAdmin()) return;
        if (!auth.isBranchOrFranchiseAdmin()) throw new AccessDeniedException("권한이 없습니다.");

        Employee actor = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

        LocalDate ref = onDate != null ? onDate : LocalDate.now();

        var myBranches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", ref, ref)
                .stream().map(DispatchStatus::getBranch).distinct().toList();

        if (myBranches.isEmpty()) throw new AccessDeniedException("권한이 없습니다.");

        boolean mine = myBranches.stream().anyMatch(b -> Objects.equals(b.getId(), branch.getId()));
        if (!mine) throw new AccessDeniedException("내 지점에 대해서만 스케줄을 관리할 수 있습니다.");

        boolean targetInBranchThatDay = dispatchStatusRepository
                .existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        targetEmployee, List.of(branch), "N", ref, ref
                );
        if (!targetInBranchThatDay) {
            throw new AccessDeniedException("해당 직원은 해당 날짜에 이 지점에 배치되어 있지 않습니다.");
        }
    }

    private void ensurePermissionForRead(Auth auth, Employee targetEmployee, LocalDate onDate) {
        if (auth.isHqAdmin()) return;

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            LocalDate ref = onDate != null ? onDate : LocalDate.now();

            var myBranches = dispatchStatusRepository
                    .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", ref, ref)
                    .stream().map(DispatchStatus::getBranch).distinct().toList();

            if (myBranches.isEmpty()) throw new AccessDeniedException("권한이 없습니다.");

            boolean ok = dispatchStatusRepository
                    .existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            targetEmployee, myBranches, "N", ref, ref
                    );

            if (!ok) throw new AccessDeniedException("내 지점 소속 직원만 조회할 수 있습니다.");
            return;
        }

        if (!Objects.equals(auth.employeeId(), targetEmployee.getId())) {
            throw new AccessDeniedException("본인 스케줄만 조회할 수 있습니다.");
        }
    }

    private LocalDateTime coalesceDateTime(LocalDateTime explicit, LocalDate date, LocalTime tmpl) {
        if (explicit != null) return explicit;
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    private LocalDateTime coalesce(LocalDate date, LocalTime explicit, LocalTime tmpl) {
        if (explicit != null) return LocalDateTime.of(date, explicit);
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    private void validateNoConflictOnSave(Employee employee, LocalDate date, Long branchId, ScheduleTypeCategory category,
                                          LocalDateTime in, LocalDateTime out, Long selfId) {

        List<Schedule> existed = scheduleRepository.findByEmployeeAndRegisteredDateIn(
                employee, List.of(date.minusDays(1), date, date.plusDays(1))
        );

        Interval newIv = toInterval(category, in, out);

        for (Schedule ex : existed) {
            if (selfId != null && Objects.equals(ex.getId(), selfId)) continue;

            Interval exIv = toInterval(ex.getScheduleType().getCategory(), ex.getRegisteredClockIn(), ex.getRegisteredClockOut());

            if (isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), branchId)) {
                throw new IllegalStateException("동일 스케줄이 이미 존재합니다.");
            }
            if (isOverlap(exIv, newIv)) {
                throw new IllegalStateException("겹치는 스케줄이 존재합니다.");
            }
        }
    }

    private Interval toInterval(ScheduleTypeCategory category, LocalDateTime in, LocalDateTime out) {
        if (category == ScheduleTypeCategory.LEAVE) {
            return Interval.allDay();
        }
        if (in == null && out == null) {
            return Interval.allDay();
        }
        if (in == null || out == null) {
            throw new IllegalArgumentException("근무 스케줄은 출근과 퇴근 시간이 모두 필요합니다.");
        }
        LocalDateTime normOut = out.isAfter(in) ? out : out.plusDays(1);
        return Interval.of(in, normOut);
    }

    private boolean isOverlap(Interval a, Interval b) {
        if (a.allDay || b.allDay) return true;
        return a.start.isBefore(b.end) && b.start.isBefore(a.end);
    }

    private boolean isExactlySame(Interval a, Interval b) {
        if (a.allDay && b.allDay) return true;
        if (a.allDay || b.allDay) return false;
        return Objects.equals(a.start, b.start) && Objects.equals(a.end, b.end);
    }

    private Map<String, Object> err(int index, String code) {
        Map<String, Object> m = new HashMap<>();
        m.put("index", index);
        m.put("code", code);
        return m;
    }

    private static class TempSlot {
        private final Long branchId;
        private final Interval interval;
        private TempSlot(Long branchId, Interval interval) {
            this.branchId = branchId;
            this.interval = interval;
        }
        public Long branchId() { return branchId; }
        public Interval interval() { return interval; }
    }

    private static class Interval {
        private final boolean allDay;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private Interval(boolean allDay, LocalDateTime start, LocalDateTime end) {
            this.allDay = allDay;
            this.start = start;
            this.end = end;
        }
        public static Interval allDay() { return new Interval(true, null, null); }
        public static Interval of(LocalDateTime start, LocalDateTime end) { return new Interval(false, start, end); }
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
