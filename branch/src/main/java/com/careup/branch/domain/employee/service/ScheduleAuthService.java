package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleAuthService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    public Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Claims claims)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long employeeId = claims.get("employeeId", Long.class);
        String rawRole = String.valueOf(claims.get("role"));
        if (employeeId == null || rawRole == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        // ROLE_ 프리픽스 제거
        String role = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;

        return new Auth(employeeId, role);
    }

    public void ensurePermissionForWrite(Auth auth, Branch branch, Employee targetEmployee, LocalDate onDate) {
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

    public void ensurePermissionForRead(Auth auth, Employee targetEmployee, LocalDate onDate) {
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

    public record Auth(Long employeeId, String role) {
        public boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        public boolean isBranchOrFranchiseAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
        public Long employeeId() { return employeeId; }
    }
}
