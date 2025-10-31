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
import com.careup.branch.domain.employee.entity.LeaveType;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.entity.WorkType;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleAuthService authz;
    private final ScheduleCommandService command;
    private final ScheduleQueryService query;
    private final ScheduleValidationService validator;

    // 챗봇용 레포지토리 의존성 유지
    private final ScheduleRepository scheduleRepository;
    private final BranchRepository branchRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;

    // 기간 기본값 계산을 서비스로 옮기기 위해 Clock 주입
    private final Clock clock;

    /* ------------------------------- 기존 공개 메서드(불변) ------------------------------- */

    @Transactional
    public ScheduleDetailDto create(ScheduleCreateDto dto) {
        var auth = authz.readAuth();
        validator.ensureActorAuthorizedForBranch(dto.getBranchId());
        validator.ensureEmployeeAssignableToBranchOnDate(dto.getEmployeeId(), dto.getBranchId(), dto.getRegisteredDate());
        return command.create(auth, dto);
    }

    @Transactional
    public List<ScheduleDetailDto> massCreate(ScheduleMassCreateDto dto) {
        var auth = authz.readAuth();
        if (dto.getBlocks() != null) {
            for (ScheduleMassBlockDto b : dto.getBlocks()) {
                validator.ensureActorAuthorizedForBranch(b.getBranchId());
                if (b.getEmployeeIds() != null && b.getDates() != null) {
                    for (Long empId : b.getEmployeeIds()) {
                        for (LocalDate d : b.getDates()) {
                            validator.ensureEmployeeAssignableToBranchOnDate(empId, b.getBranchId(), d);
                        }
                    }
                }
            }
        }
        if (dto.getItems() != null) {
            for (ScheduleMassItemDto it : dto.getItems()) {
                validator.ensureActorAuthorizedForBranch(it.getBranchId());
                validator.ensureEmployeeAssignableToBranchOnDate(it.getEmployeeId(), it.getBranchId(), it.getDate());
            }
        }
        return command.massCreate(auth, dto);
    }

    public Map<String, Object> massValidate(ScheduleMassCreateDto dto) {
        return validator.massValidate(dto);
    }

    public List<ScheduleListDto> listAll(LocalDate from, LocalDate to) {
        var auth = authz.readAuth();
        return query.listAll(auth, from, to);
    }

    public List<ScheduleListDto> listMine(LocalDate from, LocalDate to) {
        var auth = authz.readAuth();
        return query.listMine(auth, from, to);
    }

    public List<ScheduleCalendarDto> calendar(Long employeeId, String yearMonth) {
        var auth = authz.readAuth();
        return query.calendar(auth, employeeId, yearMonth);
    }

    public List<ScheduleCalendarDto> calendarRange(List<Long> employeeIds, LocalDate from, LocalDate to) {
        var auth = authz.readAuth();
        return query.calendarRange(auth, employeeIds, from, to);
    }

    public ScheduleDetailDto detail(Long scheduleId) {
        var auth = authz.readAuth();
        return query.detail(auth, scheduleId);
    }

    @Transactional
    public ScheduleDetailDto update(Long scheduleId, ScheduleUpdateDto dto) {
        var auth = authz.readAuth();
        validator.ensureActorAuthorizedForBranch(dto.getBranchId());
        return command.update(auth, scheduleId, dto);
    }

    @Transactional
    public void delete(Long scheduleId) {
        var auth = authz.readAuth();
        command.delete(auth, scheduleId);
    }

    @Transactional
    public void deleteMany(List<Long> scheduleIds) {
        var auth = authz.readAuth();
        command.deleteMany(auth, scheduleIds);
    }

    /**
     * 챗봇용 커스텀 업데이트 메서드
     */
    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleUpdateDto dto) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스케줄입니다."));
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));

        ScheduleTypeCategory category = schedule.getCategory();

        WorkType workType = null;
        if (dto.getWorkTypeId() != null) {
            workType = workTypeRepository.findById(dto.getWorkTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));
        }

        LeaveType leaveType = null;
        if (dto.getLeaveTypeId() != null) {
            leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));
        }

        AttendanceTemplate at = null;
        if (dto.getAttendanceTemplateId() != null) {
            at = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));
        }

        LocalDate date = dto.getRegisteredDate();
        LocalDateTime in = dto.getRegisteredClockIn();
        LocalDateTime out = dto.getRegisteredClockOut();

        // 기존 로직 그대로 유지(템플릿 기준 시간으로 치환)
        schedule.changeSchedule(
                branch,
                category,
                workType,
                leaveType,
                at,
                date,
                LocalDateTime.of(date, at.getDefaultClockIn()),
                LocalDateTime.of(date, at.getDefaultClockOut()),
                LocalDateTime.of(date, at.getDefaultBreakStart()),
                LocalDateTime.of(date, at.getDefaultBreakEnd())
        );
    }

    /* ------------------------------- 컨트롤러 로직 이관(신규) ------------------------------- */

    private record DateRange(LocalDate from, LocalDate to) {}

    private DateRange normalizeRangeOrThrow(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedFrom = (from == null) ? today : from;
        LocalDate resolvedTo   = (to == null) ? resolvedFrom : to;
        if (resolvedFrom.isAfter(resolvedTo)) {
            // 공통 예외 핸들러와 연동되는 표준 예외 사용
            throw new IllegalArgumentException("조회 시작일은 종료일보다 이후일 수 없습니다.");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    /**
     * 컨트롤러의 /schedule/list 에서 수행하던 기본값/검증 로직을 이관
     * - today 기본값
     * - from/to 역전 검증
     */
    public List<ScheduleListDto> listAllWithDefaults(LocalDate from, LocalDate to) {
        DateRange r = normalizeRangeOrThrow(from, to);
        return listAll(r.from, r.to);
    }

    /**
     * 컨트롤러의 /schedule/my-schedule 에서 수행하던 기본값/검증 로직을 이관
     */
    public List<ScheduleListDto> listMineWithDefaults(LocalDate from, LocalDate to) {
        DateRange r = normalizeRangeOrThrow(from, to);
        return listMine(r.from, r.to);
    }

    /**
     * 컨트롤러의 /schedule/calendar (from/to) 및 /schedule/calendar-range 에서의 기간 검증을 서비스로 이관
     */
    public List<ScheduleCalendarDto> calendarRangeValidated(List<Long> employeeIds, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("조회 기간(from/to)은 필수입니다.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 이후일 수 없습니다.");
        }
        return calendarRange(employeeIds, from, to);
    }

    /**
     * 단일 employeeId + from/to 호환용(컨트롤러의 Fallback 오버로드에서 호출)
     */
    public List<ScheduleCalendarDto> calendarRangeFallback(Long employeeId, LocalDate from, LocalDate to) {
        List<Long> ids = (employeeId != null) ? List.of(employeeId) : null;
        return calendarRangeValidated(ids, from, to);
    }
}
