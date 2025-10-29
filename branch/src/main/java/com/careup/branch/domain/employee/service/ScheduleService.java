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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.careup.branch.domain.employee.entity.*;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleAuthService authz;
    private final ScheduleCommandService command;
    private final ScheduleQueryService query;
    private final ScheduleValidationService validator;

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


        private final ScheduleRepository scheduleRepository;
        private final BranchRepository branchRepository;
        private final WorkTypeRepository workTypeRepository;
        private final LeaveTypeRepository leaveTypeRepository;
        private final AttendanceTemplateRepository attendanceTemplateRepository;
    public void updateSchedule(Long scheduleId, ScheduleUpdateDto dto){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 스케줄입니다."));
        Branch branch = branchRepository.findById(dto.getBranchId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 지점입니다."));
        ScheduleTypeCategory category= schedule.getCategory();
        WorkType workType = null;
        if(dto.getWorkTypeId()!=null){
        workType = workTypeRepository.findById(dto.getWorkTypeId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));
        }
        LeaveType leaveType = null;
        if(dto.getLeaveTypeId() !=null){
            leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));
        }

        AttendanceTemplate at = null;

        if(dto.getAttendanceTemplateId()!=null){
            at = attendanceTemplateRepository.findById(dto.getAttendanceTemplateId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 근무타입 입니다."));

        }
        LocalDate date = dto.getRegisteredDate();
        LocalDateTime in = dto.getRegisteredClockIn();
        LocalDateTime out = dto.getRegisteredClockOut();
        schedule.changeSchedule(branch, category,workType,leaveType,at,date,LocalDateTime.of(date,at.getDefaultClockIn()), LocalDateTime.of(date,at.getDefaultClockOut()), LocalDateTime.of(date,at.getDefaultBreakStart()), LocalDateTime.of(date,at.getDefaultBreakEnd()));
    }
}
