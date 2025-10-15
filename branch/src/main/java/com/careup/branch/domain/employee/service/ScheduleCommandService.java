package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.dto.request.ScheduleCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassBlockDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto;
import com.careup.branch.domain.employee.dto.request.ScheduleUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleDetailDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleType;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.ScheduleTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCommandService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final BranchRepository branchRepository;

    private final ScheduleAuthService authz;
    private final ScheduleTimeService time;
    private final ScheduleValidationService validator;

    @Transactional
    public ScheduleDetailDto create(ScheduleAuthService.Auth auth, ScheduleCreateDto dto) {
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

        authz.ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());

        LocalDate date = dto.getRegisteredDate();

        LocalDateTime in, bs, be, out;
        if (type.getCategory() == ScheduleTypeCategory.LEAVE) {
            in = bs = be = out = null;
        } else {
            in  = time.coalesceDateTime(dto.getRegisteredClockIn(),  date, template != null ? template.getDefaultClockIn()  : null);
            bs  = time.coalesceDateTime(dto.getRegisteredBreakStart(), date, template != null ? template.getDefaultBreakStart() : null);
            be  = time.coalesceDateTime(dto.getRegisteredBreakEnd(), date, template != null ? template.getDefaultBreakEnd() : null);
            out = time.coalesceDateTime(dto.getRegisteredClockOut(), date, template != null ? template.getDefaultClockOut() : null);
            if (dto.getRegisteredClockIn() != null && dto.getRegisteredClockOut() != null) {
                if (out == null || in == null || !out.isAfter(in)) throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후여야 합니다.");
            }
        }

        validator.validateNoConflictOnSave(employee, date, branch.getId(), type.getCategory(), in, out, null);

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
    public List<ScheduleDetailDto> massCreate(ScheduleAuthService.Auth auth, ScheduleMassCreateDto dto) {
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
            Employee emp = employeeMap.get(e.employeeId());
            Branch br    = branchMap.get(e.branchId());
            authz.ensurePermissionForWrite(auth, br, emp, e.date());
        }

        Set<LocalDate> dates  = entries.stream().map(Entry::date).collect(Collectors.toSet());
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

            LocalDateTime in, bs, be, out;
            if (tp.getCategory() == ScheduleTypeCategory.LEAVE) {
                in = bs = be = out = null;
            } else {
                in  = time.coalesce(e.date(), e.in(),  tmpl != null ? tmpl.getDefaultClockIn()  : null);
                bs  = time.coalesce(e.date(), e.bs(),  tmpl != null ? tmpl.getDefaultBreakStart() : null);
                be  = time.coalesce(e.date(), e.be(),  tmpl != null ? tmpl.getDefaultBreakEnd()   : null);
                out = time.coalesce(e.date(), e.out(), tmpl != null ? tmpl.getDefaultClockOut() : null);
            }

            ScheduleTimeService.Interval newIv = toInterval(tp.getCategory(), in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(emp.getId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                ScheduleTimeService.Interval exIv = toInterval(ex.getScheduleType().getCategory(), ex.getRegisteredClockIn(), ex.getRegisteredClockOut());
                if (time.isExactlySame(exIv, newIv) && Objects.equals(ex.getBranch().getId(), br.getId())) {
                    throw new IllegalStateException("동일 스케줄이 이미 존재합니다.");
                }
                if (time.isOverlap(exIv, newIv)) {
                    throw new IllegalStateException("겹치는 스케줄이 존재합니다.");
                }
            }

            List<TempSlot> empSlots = tempSlotsPerEmp.computeIfAbsent(emp.getId(), k -> new ArrayList<>());
            for (TempSlot ts : empSlots) {
                if (time.isExactlySame(ts.interval(), newIv) && Objects.equals(ts.branchId(), br.getId())) {
                    throw new IllegalStateException("요청 내 동일 스케줄이 중복되었습니다.");
                }
                if (time.isOverlap(ts.interval(), newIv)) {
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

    @Transactional
    public ScheduleDetailDto update(ScheduleAuthService.Auth auth, Long scheduleId, ScheduleUpdateDto dto) {
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

        authz.ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());

        LocalDate date = dto.getRegisteredDate();

        LocalDateTime in, bs, be, out;
        if (type.getCategory() == ScheduleTypeCategory.LEAVE) {
            in = bs = be = out = null;
        } else {
            in  = time.coalesceDateTime(dto.getRegisteredClockIn(),  date, template != null ? template.getDefaultClockIn()  : null);
            bs  = time.coalesceDateTime(dto.getRegisteredBreakStart(), date, template != null ? template.getDefaultBreakStart() : null);
            be  = time.coalesceDateTime(dto.getRegisteredBreakEnd(), date, template != null ? template.getDefaultBreakEnd() : null);
            out = time.coalesceDateTime(dto.getRegisteredClockOut(), date, template != null ? template.getDefaultClockOut() : null);
            if (dto.getRegisteredClockIn() != null && dto.getRegisteredClockOut() != null) {
                if (out == null || in == null || !out.isAfter(in)) throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후여야 합니다.");
            }
        }

        validator.validateNoConflictOnSave(employee, date, branch.getId(), type.getCategory(), in, out, target.getId());

        target.change(branch, type, template, date, in, bs, be, out);
        ScheduleEvent ev = scheduleEventRepository.findByScheduleId(target.getId()).orElse(null);
        return ScheduleDetailDto.from(target, ev);
    }

    @Transactional
    public void delete(ScheduleAuthService.Auth auth, Long scheduleId) {
        Schedule target = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("스케줄을 찾을 수 없습니다."));

        authz.ensurePermissionForRead(auth, target.getEmployee(), target.getRegisteredDate());

        boolean hasEvent = scheduleEventRepository.findByScheduleId(scheduleId).isPresent();
        if (hasEvent) throw new IllegalStateException("이미 근태 이벤트가 존재합니다. 이벤트 삭제 후 스케줄을 삭제하세요.");
        scheduleRepository.delete(target);
    }

    @Transactional
    public void deleteMany(ScheduleAuthService.Auth auth, List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 스케줄이 없습니다.");
        }

        List<Schedule> targets = scheduleRepository.findAllById(scheduleIds);
        if (targets.size() != new HashSet<>(scheduleIds).size()) {
            throw new jakarta.persistence.EntityNotFoundException("일부 스케줄을 찾을 수 없습니다.");
        }

        for (Schedule s : targets) {
            authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());
        }

        long evCount = scheduleEventRepository.countByScheduleIdIn(scheduleIds);
        if (evCount > 0) {
            throw new IllegalStateException("일부 스케줄에 근태 이벤트가 존재합니다. 이벤트 삭제 후 다시 시도하세요.");
        }

        scheduleRepository.deleteAllByIdInBatch(scheduleIds);
    }

    private void ensureUpdatable(Long scheduleId) {
        boolean hasEvent = scheduleEventRepository.findByScheduleId(scheduleId).isPresent();
        if (hasEvent) throw new IllegalStateException("이미 근태 이벤트가 존재합니다. 이벤트 삭제 후 수정하세요.");
    }

    private ScheduleTimeService.Interval toInterval(ScheduleTypeCategory category, LocalDateTime in, LocalDateTime out) {
        if (category == ScheduleTypeCategory.LEAVE) return time.allDay();
        if (in == null && out == null) return time.allDay();
        if (in == null || out == null) throw new IllegalArgumentException("근무 스케줄은 출근과 퇴근 시간이 모두 필요합니다.");
        return time.interval(in, out);
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
