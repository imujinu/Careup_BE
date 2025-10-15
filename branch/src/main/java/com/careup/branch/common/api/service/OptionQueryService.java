package com.careup.branch.common.api.service;

import com.careup.branch.common.api.dto.response.BranchOptionDto;
import com.careup.branch.common.api.dto.response.EmployeeOptionDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionQueryService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    private record Auth(Long employeeId, String role) {
        boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        boolean isBranchAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
    }

    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Claims c)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long employeeId = c.get("employeeId", Long.class);
        String rawRole = String.valueOf(c.get("role"));
        if (employeeId == null || rawRole == null) throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        String role = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        return new Auth(employeeId, role);
    }

    public List<BranchOptionDto> branchOptions(String keyword) {
        List<Branch> branches = searchBranches(keyword);
        return branches.stream()
                .map(b -> BranchOptionDto.builder().id(b.getId()).name(b.getName()).build())
                .toList();
    }

    private List<Branch> searchBranches(String keyword) {
        var auth = readAuth();
        if (auth.isHqAdmin()) {
            return (keyword == null || keyword.isBlank())
                    ? branchRepository.findAll()
                    : branchRepository.findByNameContaining(keyword.trim());
        }
        if (auth.isBranchAdmin()) {
            LocalDate today = LocalDate.now();
            var myBranches = dispatchStatusRepository
                    .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            employeeRepository.findById(auth.employeeId()).orElseThrow(),
                            "N", today, today)
                    .stream()
                    .map(DispatchStatus::getBranch)
                    .distinct()
                    .toList();
            if (keyword == null || keyword.isBlank()) return myBranches;
            String k = keyword.trim();
            return myBranches.stream().filter(b -> b.getName().contains(k)).toList();
        }
        return List.of();
    }

    public List<EmployeeOptionDto> employeeOptions(Collection<Long> branchIds, LocalDate from, LocalDate to, String keyword, boolean all) {
        List<Employee> list = searchEmployees(branchIds, from, to, keyword, all);
        var today = LocalDate.now();
        return list.stream()
                .map(e -> {
                    var branches = dispatchStatusRepository
                            .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                                    e, "N", today, today)
                            .stream()
                            .map(DispatchStatus::getBranch)
                            .map(Branch::getName)
                            .distinct()
                            .toList();
                    return EmployeeOptionDto.builder()
                            .id(e.getId())
                            .name(e.getName())
                            .employeeNumber(e.getEmployeeNumber())
                            .jobGradeName(e.getJobGrade() != null ? e.getJobGrade().getName() : null)
                            .branchNames(branches)
                            .build();
                })
                .toList();
    }

    private List<Employee> searchEmployees(Collection<Long> branchIds, LocalDate from, LocalDate to, String keyword, boolean all) {
        var auth = readAuth();
        if (from == null) from = LocalDate.now();
        if (to == null) to = from;

        if (auth.isHqAdmin() && all) {
            List<Employee> base = employeeRepository.findByEnabledTrue(Pageable.unpaged()).getContent();
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.trim().toLowerCase();
                base = base.stream().filter(e ->
                        (e.getName() != null && e.getName().toLowerCase().contains(k)) ||
                                (e.getEmployeeNumber() != null && e.getEmployeeNumber().toLowerCase().contains(k)) ||
                                (e.getEmail() != null && e.getEmail().toLowerCase().contains(k))
                ).toList();
            }
            return base;
        }

        Collection<Long> allowedBranchIds = branchIds;

        if (!auth.isHqAdmin()) {
            LocalDate today = LocalDate.now();
            var myBranchIds = dispatchStatusRepository
                    .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            employeeRepository.findById(auth.employeeId()).orElseThrow(),
                            "N", today, today)
                    .stream()
                    .map(DispatchStatus::getBranch)
                    .map(Branch::getId)
                    .collect(Collectors.toSet());
            allowedBranchIds = (branchIds == null || branchIds.isEmpty())
                    ? myBranchIds
                    : branchIds.stream().filter(myBranchIds::contains).toList();
        }

        if (auth.isHqAdmin() && (allowedBranchIds == null || allowedBranchIds.isEmpty())) {
            var allBranchIds = branchRepository.findAll().stream().map(Branch::getId).toList();
            if (allBranchIds.isEmpty()) return List.of();
            allowedBranchIds = allBranchIds;
        }

        if (allowedBranchIds == null || allowedBranchIds.isEmpty()) return List.of();

        var employees = dispatchStatusRepository.findActiveEmployeesByBranchesAndRange(allowedBranchIds, from, to);

        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            employees = employees.stream().filter(e ->
                    (e.getName() != null && e.getName().toLowerCase().contains(k)) ||
                            (e.getEmployeeNumber() != null && e.getEmployeeNumber().toLowerCase().contains(k)) ||
                            (e.getEmail() != null && e.getEmail().toLowerCase().contains(k))
            ).toList();
        }
        return employees;
    }
}
