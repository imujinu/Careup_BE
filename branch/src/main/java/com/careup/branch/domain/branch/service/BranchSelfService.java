package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.dto.branch.BranchSelfUpdateDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.OwnershipType;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchSelfService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final BranchRepository branchRepository;

    // 목록 조회 - 가맹점주
    public List<BranchDto> listMyFranchiseBranches() {
        Auth auth = readAuth();
        Employee me = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));
        List<Branch> branches = activeBranchesOf(me).stream()
                .filter(b -> b.getOwnershipType() == OwnershipType.NO)
                .distinct()
                .toList();
        return branches.stream().map(BranchDto::fromEntity).toList();
    }

    // 목록 조회 - 직영점주
    public List<BranchDto> listMyDirectBranches() {
        Auth auth = readAuth();
        Employee me = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));
        List<Branch> branches = activeBranchesOf(me).stream()
                .filter(b -> b.getOwnershipType() == OwnershipType.YES)
                .distinct()
                .toList();
        return branches.stream().map(BranchDto::fromEntity).toList();
    }

    // 상세 조회 - 가맹점주
    public BranchDto getFranchiseBranch(Long branchId) {
        Branch branch = getOwnedBranchOrThrow(branchId, OwnershipType.NO);
        return BranchDto.fromEntity(branch);
    }

    // 상세 조회 - 직영점주
    public BranchDto getDirectBranch(Long branchId) {
        Branch branch = getOwnedBranchOrThrow(branchId, OwnershipType.YES);
        return BranchDto.fromEntity(branch);
    }

    // 수정 요청 - 가맹점주 (이름/프로필만 요청 가능, 즉시 반영 안 함)
    @Transactional
    public String requestFranchiseSelfUpdate(Long branchId, BranchSelfUpdateDto dto) {
        Branch branch = getOwnedBranchOrThrow(branchId, OwnershipType.NO);
        validateSelfUpdateDto(dto);
        String note = buildApprovalRequestNote(dto);
        appendApprovalNote(branch, note);
        return note;
    }

    // 수정 요청 - 직영점주
    @Transactional
    public String requestDirectSelfUpdate(Long branchId, BranchSelfUpdateDto dto) {
        Branch branch = getOwnedBranchOrThrow(branchId, OwnershipType.YES);
        validateSelfUpdateDto(dto);
        String note = buildApprovalRequestNote(dto);
        appendApprovalNote(branch, note);
        return note;
    }

    private void appendApprovalNote(Branch branch, String note) {
        // remark 필드에 승인요청 기록만 저장 (실제 변경은 HQ 승인 후 별도 프로세스에서 적용)
        branch.appendRemark(note);
        branchRepository.save(branch);
    }

    private Branch getOwnedBranchOrThrow(Long branchId, OwnershipType requiredType) {
        Auth auth = readAuth();
        Employee me = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        List<Branch> myBranches = activeBranchesOf(me);
        Set<Long> allowed = myBranches.stream().map(Branch::getId).collect(Collectors.toSet());

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        if (!allowed.contains(branch.getId())) {
            throw new IllegalArgumentException("내 지점만 조회/수정할 수 있습니다.");
        }
        if (!Objects.equals(branch.getOwnershipType(), requiredType)) {
            throw new IllegalArgumentException("요청한 권한과 지점 유형이 일치하지 않습니다.");
        }
        return branch;
    }

    private List<Branch> activeBranchesOf(Employee employee) {
        LocalDate today = LocalDate.now();
        return dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employee, "N", today, today
                ).stream()
                .map(DispatchStatus::getBranch)
                .distinct()
                .toList();
    }

    private void validateSelfUpdateDto(BranchSelfUpdateDto dto) {
        boolean noName = (dto.getName() == null || dto.getName().isBlank());
        boolean noImage = (dto.getProfileImageUrl() == null || dto.getProfileImageUrl().isBlank());
        if (noName && noImage) {
            throw new IllegalArgumentException("수정 요청할 항목이 없습니다. (name 또는 profileImageUrl 필요)");
        }
    }

    private String buildApprovalRequestNote(BranchSelfUpdateDto dto) {
        Auth auth = readAuth();
        String now = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String namePart = (dto.getName() != null && !dto.getName().isBlank())
                ? ("\"name\":\"" + dto.getName().replace("\"", "\\\"") + "\"") : null;
        String imgPart = (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().isBlank())
                ? ("\"profileImageUrl\":\"" + dto.getProfileImageUrl().replace("\"", "\\\"") + "\"") : null;
        String fields = (namePart != null && imgPart != null) ? (namePart + "," + imgPart)
                : (namePart != null ? namePart : imgPart);
        return "[PENDING_SELF_UPDATE]{\"by\":" + auth.employeeId() + ",\"date\":\"" + now + "\",\"fields\":{" + fields + "}}";
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
        if (employeeId == null) throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        return new Auth(employeeId);
    }

    private record Auth(Long employeeId) { }
}
