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
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionQueryService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final Clock clock;

    private record Auth(Long employeeId, String role) {
        boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        boolean isBranchAdmin() { return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role); }
    }

    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (details instanceof Claims c) {
            Long employeeId = c.get("employeeId", Long.class);
            String rawRole = String.valueOf(c.get("role"));
            if (employeeId == null || rawRole == null) {
                throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
            }
            String role = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
            return new Auth(employeeId, role);
        }
        Long employeeId = null;
        try { employeeId = Long.valueOf(authentication.getName()); } catch (Exception ignored) {}
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst().orElse(null);
        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    public List<BranchOptionDto> branchOptions(String keyword) {
        List<Branch> branches = searchBranches(keyword);
        return branches.stream()
                .map(b -> BranchOptionDto.builder().id(b.getId()).name(b.getName()).build())
                .sorted(Comparator.comparing(BranchOptionDto::getId, Comparator.nullsLast(Long::compareTo)))
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
            LocalDate today = LocalDate.now(clock);
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
            return myBranches.stream().filter(b -> b.getName() != null && b.getName().contains(k)).toList();
        }
        return List.of();
    }

    public List<EmployeeOptionDto> employeeOptions(Collection<Long> branchIds, LocalDate from, LocalDate to, String keyword, boolean all) {
        List<Employee> employees = searchEmployees(branchIds, from, to, keyword, all);
        if (employees.isEmpty()) return List.of();

        LocalDate today = LocalDate.now(clock);

        Map<Long, Set<String>> empIdToBranchNames = dispatchStatusRepository
                .findByEmployeeInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employees, "N", today, today)
                .stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getEmployee().getId(),
                        Collectors.mapping(ds -> ds.getBranch() != null ? ds.getBranch().getName() : null,
                                Collectors.filtering(n -> n != null, Collectors.toSet()))
                ));

        return employees.stream()
                .map(e -> EmployeeOptionDto.builder()
                        .id(e.getId())
                        .name(e.getName())
                        .employeeNumber(e.getEmployeeNumber())
                        .jobGradeName(e.getJobGrade() != null ? e.getJobGrade().getName() : null)
                        .branchNames(empIdToBranchNames.getOrDefault(e.getId(), Set.of())
                                .stream()
                                .sorted(String::compareToIgnoreCase)
                                .toList())
                        .build())
                .sorted(Comparator.comparing(EmployeeOptionDto::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<Employee> searchEmployees(Collection<Long> branchIds, LocalDate from, LocalDate to, String keyword, boolean all) {
        var auth = readAuth();
        if (from == null) from = LocalDate.now(clock);
        if (to == null) to = from;

        if (auth.isHqAdmin() && all) {
            if (keyword != null && !keyword.isBlank()) {
                return employeeRepository.searchByKeyword(keyword.trim(), PageRequest.of(0, 200)).getContent();
            }
            return employeeRepository.findByEnabledTrue(PageRequest.of(0, 200)).getContent();
        }

        Collection<Long> allowedBranchIds = branchIds;

        if (!auth.isHqAdmin()) {
            LocalDate today = LocalDate.now(clock);
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
