package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.dto.request.DispatchAssignmentDto;
import com.careup.branch.domain.employee.dto.request.EmployeeCreateDto;
import com.careup.branch.domain.employee.dto.request.EmployeeUpdateDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDispatchDto;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.JobGrade;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JobGradeRepository jobGradeRepository;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EmployeeDetailDto create(EmployeeCreateDto dto) {
        dto.setMobile(normalizePhone(dto.getMobile()));
        dto.setEmergencyTel(normalizePhone(dto.getEmergencyTel()));
        validateDuplicateOnCreate(dto);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            enforceBranchManagePermission(dto.getDispatches(), actorId);
        }

        JobGrade jobGrade = null;
        if (dto.getJobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(dto.getJobGradeId())
                    .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        }

        Employee employee = Employee.builder()
                .employeeNumber(dto.getEmployeeNumber())
                .name(dto.getName())
                .jobGrade(jobGrade)
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .zipcode(dto.getZipcode())
                .address(dto.getAddress())
                .addressDetail(dto.getAddressDetail())
                .mobile(dto.getMobile())
                .emergencyTel(dto.getEmergencyTel())
                .emergencyName(dto.getEmergencyName())
                .relationship(dto.getRelationship())
                .hireDate(dto.getHireDate())
                .terminateDate(dto.getTerminateDate())
                .authorityType(dto.getAuthorityType())
                .employmentStatus(dto.getEmploymentStatus())
                .employmentType(dto.getEmploymentType())
                .profileImageUrl(dto.getProfileImageUrl())
                .remark(dto.getRemark())
                .passwordHash(passwordEncoder.encode(dto.getRawPassword()))
                .enabled(true)
                .build();

        Employee saved = employeeRepository.save(employee);
        List<EmployeeDispatchDto> dispatchDtos = saveAllDispatches(saved, dto.getDispatches());
        return EmployeeDetailDto.fromEntity(saved, dispatchDtos);
    }

    @Transactional
    public EmployeeDetailDto update(Long id, EmployeeUpdateDto dto) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        if (!dto.getEmployeeNumber().equals(e.getEmployeeNumber())) {
            throw new IllegalArgumentException("사번은 변경할 수 없습니다.");
        }

        dto.setMobile(normalizePhone(dto.getMobile()));
        dto.setEmergencyTel(normalizePhone(dto.getEmergencyTel()));
        validateDuplicateOnUpdate(e, dto);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            ensureTargetBelongsToMyBranches(e.getId(), actorId, "수정");
            enforceBranchManagePermission(dto.getDispatches(), actorId);
        }

        JobGrade jobGrade = null;
        if (dto.getJobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(dto.getJobGradeId())
                    .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        }

        e.updateFromDto(dto, jobGrade);

        if (dto.getRawPassword() != null && !dto.getRawPassword().isBlank()) {
            String encoded = passwordEncoder.encode(dto.getRawPassword());
            try {
                var f = Employee.class.getDeclaredField("passwordHash");
                f.setAccessible(true);
                f.set(e, encoded);
            } catch (Exception ex) {
                throw new IllegalStateException("비밀번호 변경 중 오류가 발생했습니다.");
            }
        }

        dispatchStatusRepository.deleteByEmployeeId(e.getId());
        List<EmployeeDispatchDto> dispatchDtos = saveAllDispatches(e, dto.getDispatches());
        return EmployeeDetailDto.fromEntity(e, dispatchDtos);
    }

    @Transactional
    public void delete(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            ensureTargetBelongsToMyBranches(e.getId(), actorId, "삭제");
        }

        try {
            employeeRepository.delete(e);
            employeeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new EntityExistsException("해당 직원과 연관된 배치/근태/기록이 있어 삭제할 수 없습니다.");
        }
    }

    private void ensureTargetBelongsToMyBranches(Long targetEmployeeId, Long actorId, String actionKor) {
        List<Long> myBranches = dispatchStatusRepository.findManageableBranchIds(actorId, LocalDate.now());
        if (myBranches == null || myBranches.isEmpty()) {
            throw new IllegalArgumentException(actionKor + " 권한이 없습니다.");
        }
        long cnt = dispatchStatusRepository.countActiveInBranches(targetEmployeeId, myBranches, LocalDate.now());
        if (cnt == 0) {
            throw new IllegalArgumentException("내 지점 소속 직원만 " + actionKor + "할 수 있습니다.");
        }
    }

    private void enforceBranchManagePermission(List<DispatchAssignmentDto> dispatches, Long actorId) {
        List<Long> allowedIds = dispatchStatusRepository.findManageableBranchIds(actorId, LocalDate.now());
        Set<Long> allowed = new HashSet<>(allowedIds);

        Set<Long> requested = (dispatches == null || dispatches.isEmpty())
                ? Set.of()
                : dispatches.stream().map(DispatchAssignmentDto::getBranchId).collect(Collectors.toSet());

        if (!allowed.containsAll(requested)) {
            throw new IllegalArgumentException("권한이 없는 지점이 포함되어 있습니다.");
        }
    }

    private List<EmployeeDispatchDto> saveAllDispatches(Employee employee, List<DispatchAssignmentDto> dispatches) {
        List<EmployeeDispatchDto> dtos = new ArrayList<>();
        if (dispatches == null || dispatches.isEmpty()) return dtos;

        for (DispatchAssignmentDto d : dispatches) {
            validateDispatchDates(d.getAssignedFrom(), d.getAssignedTo());

            Branch branch = branchRepository.findById(d.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

            String placementYn = (d.getPlacementYn() == null || d.getPlacementYn().isBlank()) ? "Y" : d.getPlacementYn();

            DispatchStatus status = DispatchStatus.builder()
                    .employee(employee)
                    .branch(branch)
                    .assignedFrom(d.getAssignedFrom())
                    .assignedTo(d.getAssignedTo())
                    .placementYn(placementYn)
                    .build();

            DispatchStatus savedStatus = dispatchStatusRepository.save(status);
            dtos.add(EmployeeDispatchDto.fromEntity(savedStatus));
        }
        return dtos;
    }

    private void validateDuplicateOnCreate(EmployeeCreateDto dto) {
        employeeRepository.findByEmployeeNumber(dto.getEmployeeNumber())
                .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 사번입니다."); });
        employeeRepository.findByEmailIgnoreCase(dto.getEmail())
                .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 이메일입니다."); });
        employeeRepository.findByMobile(dto.getMobile())
                .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다."); });
    }

    private void validateDuplicateOnUpdate(Employee current, EmployeeUpdateDto dto) {
        if (!dto.getEmail().equalsIgnoreCase(current.getEmail())) {
            employeeRepository.findByEmailIgnoreCase(dto.getEmail())
                    .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 이메일입니다."); });
        }
        if (!dto.getMobile().equals(current.getMobile())) {
            employeeRepository.findByMobile(dto.getMobile())
                    .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다."); });
        }
    }

    private void validateDispatchDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("배치 시작/종료일이 필요합니다.");
        if (from.isAfter(to)) throw new IllegalArgumentException("배치 시작일이 종료일보다 늦을 수 없습니다.");
    }

    private String normalizePhone(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 11) return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7);
        if (digits.length() == 10) return digits.substring(0,3) + "-" + digits.substring(3,6) + "-" + digits.substring(6);
        return input;
    }
}
