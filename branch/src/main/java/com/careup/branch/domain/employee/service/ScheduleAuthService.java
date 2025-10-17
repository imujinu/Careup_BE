package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleAuthService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    public record Auth(Long employeeId, String role) {
        public boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        public boolean isBranchOrFranchiseAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
        public boolean isStaff() { return "STAFF".equals(role); }
    }

    /** 커스텀 JwtTokenFilter가 SecurityContext에 심어둔 Claims 기반 인증 정보 읽기 */
    public Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        // 1) our standard path: details 에 io.jsonwebtoken.Claims 가 들어있음
        Object details = authentication.getDetails();
        if (details instanceof Claims c) {
            Long employeeId = c.get("employeeId", Long.class);
            String role = String.valueOf(c.get("role"));
            if (employeeId == null || role == null) {
                throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
            }
            return new Auth(employeeId, role);
        }

        // 2) 폴백: 테스트나 특수상황에서 details가 비어있을 수 있으므로 principal/authorities로 복구
        Long employeeId = null;
        try {
            employeeId = Long.valueOf(authentication.getName()); // subject 를 숫자 ID로 넣었을 때만 유효
        } catch (Exception ignored) {}

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)            // ex) ROLE_HQ_ADMIN
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst().orElse(null);

        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    /** 스케줄 읽기 권한: HQ=전부, 지점/가맹=내 지점 소속 직원, Staff=본인만 */
    public void ensurePermissionForRead(Auth auth, Employee targetEmployee, LocalDate date) {
        if (auth.isHqAdmin()) return;

        if (auth.isBranchOrFranchiseAdmin()) {
            if (!isEmployeeWithinMyBranchesOn(auth.employeeId(), targetEmployee, date)) {
                throw new AccessDeniedException("권한이 없습니다.");
            }
            return;
        }

        // STAFF
        if (!Objects.equals(auth.employeeId(), targetEmployee.getId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    /** 스케줄 쓰기 권한: HQ=전부, 지점/가맹=내 지점 & 대상 직원도 내 지점, Staff=본인 & 그 날 자신의 지점 */
    public void ensurePermissionForWrite(Auth auth, Branch targetBranch, Employee targetEmployee, LocalDate date) {
        if (auth.isHqAdmin()) return;

        if (auth.isBranchOrFranchiseAdmin()) {
            if (!isMyBranchOn(auth.employeeId(), targetBranch, date)) {
                throw new AccessDeniedException("권한이 없습니다.");
            }
            if (!isEmployeeWithinMyBranchesOn(auth.employeeId(), targetEmployee, date)) {
                throw new AccessDeniedException("권한이 없습니다.");
            }
            return;
        }

        // STAFF
        if (!Objects.equals(auth.employeeId(), targetEmployee.getId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        if (!isEmployeeInBranchOn(targetEmployee, targetBranch, date)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    /* =========================
              Helpers
       ========================= */

    private boolean isMyBranchOn(Long adminId, Branch branch, LocalDate date) {
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));
        List<DispatchStatus> myAssignments = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        admin, "N", date, date
                );
        return myAssignments.stream()
                .map(DispatchStatus::getBranch)
                .filter(Objects::nonNull)
                .anyMatch(b -> Objects.equals(b.getId(), branch.getId()));
    }

    private boolean isEmployeeWithinMyBranchesOn(Long adminId, Employee target, LocalDate date) {
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

        List<Branch> myBranches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        admin, "N", date, date
                )
                .stream()
                .map(DispatchStatus::getBranch)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (myBranches.isEmpty()) return false;

        return dispatchStatusRepository
                .existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        target, myBranches, "N", date, date
                );
    }

    private boolean isEmployeeInBranchOn(Employee emp, Branch branch, LocalDate date) {
        return dispatchStatusRepository
                .existsByEmployee_IdAndBranch_IdAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        emp.getId(), branch.getId(), "N", date, date
                );
    }

    // (선택) 같은 지점인지 빠르게 비교해야 할 때 사용
    private boolean isSameBranchOn(Long adminId, Employee target, LocalDate date) {
        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

        Optional<DispatchStatus> adminDs = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        admin, "N", date, date
                );
        Optional<DispatchStatus> targetDs = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        target, "N", date, date
                );

        return adminDs.isPresent() && targetDs.isPresent()
                && adminDs.get().getBranch() != null
                && targetDs.get().getBranch() != null
                && Objects.equals(adminDs.get().getBranch().getId(), targetDs.get().getBranch().getId());
    }
}
