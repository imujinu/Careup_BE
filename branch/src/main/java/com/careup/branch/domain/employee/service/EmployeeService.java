package com.careup.branch.domain.employee.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.common.util.PhoneUtils;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final AwsS3Uploader awsS3Uploader;

    private static final String DEFAULT_PROFILE_URL =
            "https://beyond-16-care-up.s3.ap-northeast-2.amazonaws.com/image/employee/profile/default/default_user.png";

    @Transactional
    public EmployeeDetailDto create(EmployeeCreateDto dto, MultipartFile image) {
        // 1) 입력 정규화(불변 DTO이므로 지역변수로만 보관)
        final String normalizedMobile = PhoneUtils.normalize(dto.getMobile());
        final String normalizedEmergencyTel = PhoneUtils.normalize(dto.getEmergencyTel());

        validateDuplicateOnCreate(dto);

        // 2) 권한 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        Employee actor = null;
        if (!"HQ_ADMIN".equals(role)) {
            actor = employeeRepository.findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            enforceBranchManagePermission(dto.getDispatches(), actor);
        }

        // 3) 참조 로딩
        JobGrade jobGrade = null;
        if (dto.getJobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(dto.getJobGradeId())
                    .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        }

        // 4) 프로필 초기 URL
        final String initialUrl = (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().isBlank())
                ? dto.getProfileImageUrl()
                : DEFAULT_PROFILE_URL;

        // 5) 직원 저장
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
                .mobile(normalizedMobile)
                .emergencyTel(normalizedEmergencyTel)
                .emergencyName(dto.getEmergencyName())
                .relationship(dto.getRelationship())
                .hireDate(dto.getHireDate())
                .terminateDate(dto.getTerminateDate())
                .authorityType(dto.getAuthorityType())
                .employmentStatus(dto.getEmploymentStatus())
                .employmentType(dto.getEmploymentType())
                .profileImageUrl(initialUrl)
                .remark(dto.getRemark())
                .passwordHash(passwordEncoder.encode(dto.getRawPassword()))
                .enabled(true)
                .build();

        Employee saved = employeeRepository.save(employee);

        // 6) 이미지 업로드(있다면) 후 URL 교체 — DTO 변경 없이 엔티티 메서드로만 변경
        if (image != null && !image.isEmpty()) {
            String dir = "employee/profile";
            String uploadedUrl = awsS3Uploader.uploadFile(dir, saved.getId(), image);
            saved.changeProfileImageUrl(uploadedUrl);
        }

        // 7) 배치 저장
        List<EmployeeDispatchDto> dispatchDtos = saveAllDispatches(saved, dto.getDispatches());
        return EmployeeDetailDto.fromEntity(saved, dispatchDtos);
    }

    @Transactional
    public EmployeeDetailDto update(Long id, EmployeeUpdateDto dto, MultipartFile image) {
        Employee target = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        if (!dto.getEmployeeNumber().equals(target.getEmployeeNumber())) {
            throw new IllegalArgumentException("사번은 변경할 수 없습니다.");
        }

        // 1) 입력 정규화(불변 DTO → 지역변수)
        final String normalizedMobile = PhoneUtils.normalize(dto.getMobile());
        final String normalizedEmergencyTel = PhoneUtils.normalize(dto.getEmergencyTel());

        validateDuplicateOnUpdate(target, dto.getEmail(), normalizedMobile);

        // 2) 권한 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            Employee actor = employeeRepository.findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            ensureTargetBelongsToMyBranches(target, actor, "수정");
            enforceBranchManagePermission(dto.getDispatches(), actor);
        }

        // 3) 참조 로딩
        JobGrade jobGrade = null;
        if (dto.getJobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(dto.getJobGradeId())
                    .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        }

        // 4) 프로필 이미지 처리(최종 URL 결정)
        final String oldUrl = target.getProfileImageUrl();
        String newUploadedUrl = null;
        String finalProfileUrl;

        if (image != null && !image.isEmpty()) {
            String dir = "employee/profile";
            newUploadedUrl = awsS3Uploader.uploadFile(dir, target.getId(), image);
            finalProfileUrl = newUploadedUrl;
        } else {
            finalProfileUrl = (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().isBlank())
                    ? dto.getProfileImageUrl()
                    : target.getProfileImageUrl();
        }

        // 5) 엔티티 변경 — DTO를 새로 빌드해 전달(불변 유지)
        EmployeeUpdateDto dto2 = EmployeeUpdateDto.builder()
                .employeeNumber(dto.getEmployeeNumber())
                .name(dto.getName())
                .jobGradeId(dto.getJobGradeId())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .zipcode(dto.getZipcode())
                .address(dto.getAddress())
                .addressDetail(dto.getAddressDetail())
                .mobile(normalizedMobile)                // 정규화 반영
                .emergencyTel(normalizedEmergencyTel)    // 정규화 반영
                .emergencyName(dto.getEmergencyName())
                .relationship(dto.getRelationship())
                .hireDate(dto.getHireDate())
                .terminateDate(dto.getTerminateDate())
                .authorityType(dto.getAuthorityType())
                .employmentStatus(dto.getEmploymentStatus())
                .employmentType(dto.getEmploymentType())
                .profileImageUrl(finalProfileUrl)        // 최종 URL 반영
                .remark(dto.getRemark())
                .rawPassword(dto.getRawPassword())
                .dispatches(dto.getDispatches())
                .build();

        target.updateFromDto(dto2, jobGrade);

        // 6) 이전 이미지 삭제(새 업로드가 있었고 URL이 바뀐 경우만)
        if (newUploadedUrl != null && !isDefaultImage(oldUrl) && !Objects.equals(oldUrl, newUploadedUrl)) {
            safeDeleteOldImage(oldUrl);
        }

        // 7) 비밀번호 변경(있다면)
        if (dto.getRawPassword() != null && !dto.getRawPassword().isBlank()) {
            target.changePasswordHash(passwordEncoder.encode(dto.getRawPassword()));
        }

        // 8) 배치 갱신
        dispatchStatusRepository.deleteByEmployee(target);
        List<EmployeeDispatchDto> dispatchDtos = saveAllDispatches(target, dto.getDispatches());
        return EmployeeDetailDto.fromEntity(target, dispatchDtos);
    }

    @Transactional
    public void deactivate(Long id) {
        Employee target = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            Employee actor = employeeRepository.findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            ensureTargetBelongsToMyBranches(target, actor, "삭제");
        }

        target.deactivate(LocalDate.now());
    }

    @Transactional
    public EmployeeDetailDto rehire(Long id) {
        Employee target = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims c = (Claims) auth.getDetails();
        String role = String.valueOf(c.get("role"));
        Long actorId = c.get("employeeId", Long.class);

        if (!"HQ_ADMIN".equals(role)) {
            Employee actor = employeeRepository.findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
            ensureRehirePermission(target, actor);
        }

        target.rehire(LocalDate.now());

        List<DispatchStatus> actives = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        target, "N", LocalDate.now(), LocalDate.now()
                );
        List<EmployeeDispatchDto> dispatchDtos = actives.stream()
                .map(EmployeeDispatchDto::fromEntity)
                .toList();

        return EmployeeDetailDto.fromEntity(target, dispatchDtos);
    }

    // ===== 내부 유틸 =====

    private boolean isDefaultImage(String url) {
        return url != null && url.equals(DEFAULT_PROFILE_URL);
    }

    private void safeDeleteOldImage(String url) {
        try {
            awsS3Uploader.deleteByUrl(url);
        } catch (Exception ignored) {
        }
    }

    private void ensureRehirePermission(Employee target, Employee actor) {
        LocalDate today = LocalDate.now();

        List<Branch> myBranches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", today, today)
                .stream()
                .map(DispatchStatus::getBranch)
                .distinct()
                .toList();

        if (myBranches.isEmpty()) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        Optional<DispatchStatus> current = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        target, "N", today, today
                );

        Branch baseBranch = current
                .map(DispatchStatus::getBranch)
                .orElseGet(() -> {
                    List<DispatchStatus> history = dispatchStatusRepository.findAllByEmployeeOrderByAssignedFromDesc(target);
                    return history.isEmpty() ? null : history.get(0).getBranch();
                });

        if (baseBranch == null) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        Set<Long> allowed = myBranches.stream().map(Branch::getId).collect(Collectors.toSet());
        if (!allowed.contains(baseBranch.getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
    }

    private void ensureTargetBelongsToMyBranches(Employee target, Employee actor, String actionKor) {
        LocalDate today = LocalDate.now();

        List<Branch> myBranches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", today, today)
                .stream()
                .map(DispatchStatus::getBranch)
                .distinct()
                .toList();

        if (myBranches.isEmpty()) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        boolean ok = dispatchStatusRepository
                .existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        target, myBranches, "N", today, today
                );

        if (!ok) {
            throw new IllegalArgumentException("내 지점 소속 직원만 " + actionKor + "할 수 있습니다.");
        }
    }

    private void enforceBranchManagePermission(List<DispatchAssignmentDto> dispatches, Employee actor) {
        if (dispatches == null || dispatches.isEmpty()) return;

        LocalDate today = LocalDate.now();

        Set<Long> requested = dispatches.stream()
                .map(DispatchAssignmentDto::getBranchId)
                .collect(Collectors.toSet());

        List<Branch> myBranches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(actor, "N", today, today)
                .stream()
                .map(DispatchStatus::getBranch)
                .distinct()
                .toList();

        Set<Long> allowed = myBranches.stream().map(Branch::getId).collect(Collectors.toSet());
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

            String placementYn = (d.getPlacementYn() == null || d.getPlacementYn().isBlank()) ? "N" : d.getPlacementYn();

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
        // mobile은 정규화 후 중복 검사 필요 → create에서는 정규화해 저장하므로, 저장 전 검사 시에는 원문 중복 허용 가능
        employeeRepository.findByMobile(PhoneUtils.normalize(dto.getMobile()))
                .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다."); });
    }

    private void validateDuplicateOnUpdate(Employee current, String newEmail, String newMobileNormalized) {
        if (!newEmail.equalsIgnoreCase(current.getEmail())) {
            employeeRepository.findByEmailIgnoreCase(newEmail)
                    .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 이메일입니다."); });
        }
        if (!newMobileNormalized.equals(current.getMobile())) {
            employeeRepository.findByMobile(newMobileNormalized)
                    .ifPresent(e -> { throw new IllegalArgumentException("이미 사용 중인 휴대전화 번호입니다."); });
        }
    }

    private void validateDispatchDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("배치 시작/종료일이 필요합니다.");
        if (from.isAfter(to)) throw new IllegalArgumentException("배치 시작일이 종료일보다 늦을 수 없습니다.");
    }
}
