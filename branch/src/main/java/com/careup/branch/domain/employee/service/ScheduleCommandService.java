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
import com.careup.branch.domain.employee.entity.LeaveType;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.entity.WorkType;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCommandService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final BranchRepository branchRepository;

    private final ScheduleAuthService authz;
    private final ScheduleTimeService time;
    private final ScheduleValidationService validator;

    private LocalTime pick(LocalTime overrideTime, LocalTime templateTime) {
        return overrideTime != null ? overrideTime : templateTime;
    }
    private LocalDateTime at(LocalDate d, LocalTime t) {
        return (t == null) ? null : LocalDateTime.of(d, t);
    }
    private LocalDateTime normalizeAfter(LocalDateTime prev, LocalDate d, LocalTime t) {
        if (t == null) return null;
        LocalDateTime cand = LocalDateTime.of(d, t);
        if (prev != null && !cand.isAfter(prev)) cand = cand.plusDays(1);
        return cand;
    }
    private static record Seq(LocalDateTime in, LocalDateTime bs, LocalDateTime be, LocalDateTime out) {}
    private Seq buildSeq(LocalDate baseDate,
                         LocalTime inT, LocalTime bsT, LocalTime beT, LocalTime outT,
                         AttendanceTemplate tpl) {
        LocalTime inPick  = pick(inT,  tpl != null ? tpl.getDefaultClockIn()   : null);
        LocalTime bsPick  = pick(bsT,  tpl != null ? tpl.getDefaultBreakStart(): null);
        LocalTime bePick  = pick(beT,  tpl != null ? tpl.getDefaultBreakEnd()  : null);
        LocalTime outPick = pick(outT, tpl != null ? tpl.getDefaultClockOut()  : null);

        LocalDateTime in  = at(baseDate, inPick);
        LocalDateTime bs  = normalizeAfter(in,  baseDate, bsPick);
        LocalDateTime be  = normalizeAfter(bs != null ? bs : in, baseDate, bePick);
        LocalDateTime out = normalizeAfter(be != null ? be : (bs != null ? bs : in), baseDate, outPick);

        return new Seq(in, bs, be, out);
    }

    @Transactional
    public ScheduleDetailDto create(ScheduleAuthService.Auth auth, ScheduleCreateDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        boolean isWork = dto.getWorkTypeId() != null;
        boolean isLeave = dto.getLeaveTypeId() != null;
        if (isWork == isLeave) {
            throw new IllegalArgumentException("workTypeId 또는 leaveTypeId 중 하나만 지정해야 합니다.");
        }
        ScheduleTypeCategory category = isWork ? ScheduleTypeCategory.WORK : ScheduleTypeCategory.LEAVE;

        WorkType workType = null;
        LeaveType leaveType = null;
        if (isWork) {
            workType = workTypeRepository.findById(dto.getWorkTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("근무 종류를 찾을 수 없습니다."));
        } else {
            leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("휴가 종류를 찾을 수 없습니다."));
        }

        AttendanceTemplate template = null;
        if (dto.getAttendanceTemplateId() != null) {
            template = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("템플릿을 찾을 수 없습니다."));
        }

        authz.ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());
        LocalDate date = dto.getRegisteredDate();

        LocalDateTime in, bs, be, out;
        if (isLeave) {
            in = bs = be = out = null;
        } else {
            if (dto.getRegisteredClockIn() != null
                    || dto.getRegisteredBreakStart() != null
                    || dto.getRegisteredBreakEnd() != null
                    || dto.getRegisteredClockOut() != null) {

                in  = dto.getRegisteredClockIn();
                bs  = dto.getRegisteredBreakStart();
                be  = dto.getRegisteredBreakEnd();
                out = dto.getRegisteredClockOut();

                if (bs  != null && in  != null && !bs.isAfter(in))  bs  = bs.plusDays(1);
                if (be  != null && bs  != null && !be.isAfter(bs))  be  = be.plusDays(1);
                LocalDateTime pivot = be != null ? be : (bs != null ? bs : in);
                if (out != null && pivot != null && !out.isAfter(pivot)) out = out.plusDays(1);

            } else {
                LocalTime inT  = null, bsT = null, beT = null, outT = null;
                var seq = buildSeq(date, inT, bsT, beT, outT, template);
                in = seq.in(); bs = seq.bs(); be = seq.be(); out = seq.out();
            }

            if (in != null && out != null && !out.isAfter(in)) {
                throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후(익일 포함)여야 합니다.");
            }
        }

        validator.validateNoConflictOnSave(employee, date, branch.getId(), isLeave, in, out, null);

        Schedule saved = scheduleRepository.save(
                Schedule.builder()
                        .branch(branch)
                        .employee(employee)
                        .category(category)
                        .workType(workType)
                        .leaveType(leaveType)
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
        record Entry(Long employeeId, Long branchId, Long workTypeId, Long leaveTypeId, Long attendanceTemplateId,
                     LocalDate date, LocalTime in, LocalTime bs, LocalTime be, LocalTime out) {}

        List<Entry> entries = new ArrayList<>();

        if (dto.getBlocks() != null) {
            for (ScheduleMassBlockDto b : dto.getBlocks()) {
                for (Long empId : b.getEmployeeIds()) {
                    for (LocalDate d : b.getDates()) {
                        entries.add(new Entry(
                                empId, b.getBranchId(), b.getWorkTypeId(), b.getLeaveTypeId(), b.getAttendanceTemplateId(),
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
                        it.getEmployeeId(), it.getBranchId(), it.getWorkTypeId(), it.getLeaveTypeId(), it.getAttendanceTemplateId(),
                        it.getDate(), it.getRegisteredClockInTime(), it.getRegisteredBreakStartTime(),
                        it.getRegisteredBreakEndTime(), it.getRegisteredClockOutTime()
                ));
            }
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("등록할 항목이 없습니다.");

        Set<Long> employeeIds = entries.stream().map(Entry::employeeId).collect(Collectors.toSet());
        Set<Long> branchIds   = entries.stream().map(Entry::branchId).collect(Collectors.toSet());
        Set<Long> workIds     = entries.stream().map(Entry::workTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> leaveIds    = entries.stream().map(Entry::leaveTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> tmplIds     = entries.stream().map(Entry::attendanceTemplateId).filter(Objects::nonNull).collect(Collectors.toSet());

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

        for (Entry e : entries) {
            if (!employeeMap.containsKey(e.employeeId())) throw new EntityNotFoundException("직원 없음: " + e.employeeId());
            if (!branchMap.containsKey(e.branchId())) throw new EntityNotFoundException("지점 없음: " + e.branchId());
            boolean isWork = e.workTypeId() != null;
            boolean isLeave = e.leaveTypeId() != null;
            if (isWork == isLeave) throw new IllegalArgumentException("workTypeId 또는 leaveTypeId 중 하나만 지정해야 합니다.");

            if (isWork && !workMap.containsKey(e.workTypeId())) throw new EntityNotFoundException("근무 종류 없음: " + e.workTypeId());
            if (isLeave && !leaveMap.containsKey(e.leaveTypeId())) throw new EntityNotFoundException("휴가 종류 없음: " + e.leaveTypeId());
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
            WorkType wt  = e.workTypeId()  != null ? workMap.get(e.workTypeId())   : null;
            LeaveType lt = e.leaveTypeId() != null ? leaveMap.get(e.leaveTypeId()) : null;
            AttendanceTemplate tmpl = e.attendanceTemplateId() != null ? tmplMap.get(e.attendanceTemplateId()) : null;

            boolean isLeave = (lt != null);
            ScheduleTypeCategory category = isLeave ? ScheduleTypeCategory.LEAVE : ScheduleTypeCategory.WORK;

            LocalDateTime in, bs, be, out;
            if (isLeave) {
                in = e.date().atStartOfDay();
                bs = null; be = null;
                out = e.date().atTime(ScheduleValidationService.LEAVE_END_CUTOFF);
            } else {
                var seq = buildSeq(e.date(), e.in(), e.bs(), e.be(), e.out(), tmpl);
                in = seq.in(); bs = seq.bs(); be = seq.be(); out = seq.out();

                if (in == null && out == null) {
                    var span = time.daySpan(e.date());
                    in = span.start(); out = span.end();
                }
                if (in != null && out != null && !out.isAfter(in)) {
                    out = out.plusDays(1);
                }
            }

            ScheduleTimeService.Interval newIv = toInterval(e.date(), isLeave, in, out);

            List<Schedule> existedForEmp = existedByEmp.getOrDefault(emp.getId(), List.of()).stream()
                    .filter(s -> {
                        LocalDate sd = s.getRegisteredDate();
                        return sd.isEqual(e.date()) || sd.isEqual(e.date().minusDays(1)) || sd.isEqual(e.date().plusDays(1));
                    })
                    .toList();

            for (Schedule ex : existedForEmp) {
                ScheduleTimeService.Interval exIv = toInterval(
                        ex.getRegisteredDate(),
                        ex.getCategory() == ScheduleTypeCategory.LEAVE,
                        ex.getRegisteredClockIn(),
                        ex.getRegisteredClockOut()
                );
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
                            .category(category)
                            .workType(wt)
                            .leaveType(lt)
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

        Employee employee = target.getEmployee();
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        boolean isWork = dto.getWorkTypeId() != null;
        boolean isLeave = dto.getLeaveTypeId() != null;
        if (isWork == isLeave) {
            throw new IllegalArgumentException("workTypeId 또는 leaveTypeId 중 하나만 지정해야 합니다.");
        }
        ScheduleTypeCategory category = isWork ? ScheduleTypeCategory.WORK : ScheduleTypeCategory.LEAVE;

        WorkType workType = null;
        LeaveType leaveType = null;
        if (isWork) {
            workType = workTypeRepository.findById(dto.getWorkTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("근무 종류를 찾을 수 없습니다."));
        } else {
            leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("휴가 종류를 찾을 수 없습니다."));
        }

        AttendanceTemplate template = null;
        if (dto.getAttendanceTemplateId() != null) {
            template = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("템플릿을 찾을 수 없습니다."));
        }

        authz.ensurePermissionForWrite(auth, branch, employee, dto.getRegisteredDate());
        LocalDate date = dto.getRegisteredDate();

        LocalDateTime in, bs, be, out;
        if (isLeave) {
            in = bs = be = out = null;
        } else {
            if (dto.getRegisteredClockIn() != null
                    || dto.getRegisteredBreakStart() != null
                    || dto.getRegisteredBreakEnd() != null
                    || dto.getRegisteredClockOut() != null) {

                in  = dto.getRegisteredClockIn();
                bs  = dto.getRegisteredBreakStart();
                be  = dto.getRegisteredBreakEnd();
                out = dto.getRegisteredClockOut();

                if (bs  != null && in  != null && !bs.isAfter(in))  bs  = bs.plusDays(1);
                if (be  != null && bs  != null && !be.isAfter(bs))  be  = be.plusDays(1);
                LocalDateTime pivot = be != null ? be : (bs != null ? bs : in);
                if (out != null && pivot != null && !out.isAfter(pivot)) out = out.plusDays(1);

            } else {
                LocalTime inT  = null, bsT = null, beT = null, outT = null;
                var seq = buildSeq(date, inT, bsT, beT, outT, template);
                in = seq.in(); bs = seq.bs(); be = seq.be(); out = seq.out();
            }

            if (in != null && out != null && !out.isAfter(in)) {
                throw new IllegalArgumentException("퇴근 시각은 출근 시각 이후(익일 포함)여야 합니다.");
            }
        }

        // [CHANGED] 업데이트 시에는 겹침/중복 검증을 수행하지 않습니다(요청사항: 수정 자유 허용).
        // validator.validateNoConflictOnSave(employee, date, branch.getId(), isLeave, in, out, target.getId());

        target.change(branch, category, workType, leaveType, template, date, in, bs, be, out);
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
            throw new EntityNotFoundException("일부 스케줄을 찾을 수 없습니다.");
        }
        for (Schedule s : targets) {
            authz.ensurePermissionForRead(auth, s.getEmployee(), s.getRegisteredDate());
        }
        long evCount = scheduleEventRepository.countByScheduleIdIn(scheduleIds);
        if (evCount > 0) throw new IllegalStateException("일부 스케줄에 근태 이벤트가 존재합니다. 이벤트 삭제 후 다시 시도하세요.");
        scheduleRepository.deleteAllByIdInBatch(scheduleIds);
    }

    private ScheduleTimeService.Interval toInterval(LocalDate baseDate, boolean isLeave, LocalDateTime in, LocalDateTime out) {
        if (isLeave) {
            return time.interval(baseDate.atStartOfDay(), baseDate.atTime(ScheduleValidationService.LEAVE_END_CUTOFF));
        }
        if (in == null && out == null) return time.daySpan(baseDate);
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
