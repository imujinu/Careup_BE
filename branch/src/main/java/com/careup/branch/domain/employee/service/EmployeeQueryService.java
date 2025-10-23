package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDispatchDto;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
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
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final com.careup.branch.domain.branch.repository.BranchRepository branchRepository;

    // 상세 조회
    public EmployeeDetailDto getDetail(Long targetEmployeeId) {
        Auth auth = readAuth();
        LocalDate today = LocalDate.now();

        Employee target = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        if (auth.isHqAdmin()) {
            return buildDetail(target);
        }

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            List<Branch> myBranches = activeBranchesOf(actor, today);
            if (myBranches.isEmpty()) throw new AccessDeniedException("권한이 없습니다.");

            boolean ok = dispatchStatusRepository
                    .existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            target, myBranches, "N", today, today
                    );
            if (!ok) throw new AccessDeniedException("내 지점 소속 직원만 조회할 수 있습니다.");

            return buildDetail(target);
        }

        // STAFF: 본인만
        if (!Objects.equals(auth.employeeId(), targetEmployeeId)) {
            throw new AccessDeniedException("본인 정보만 조회할 수 있습니다.");
        }
        return buildDetail(target);
    }

    // 목록 조회 (관리자 전용)
    public Page<EmployeeDetailDto> list(Pageable pageable) {
        Auth auth = readAuth();
        LocalDate today = LocalDate.now();

        if (auth.isHqAdmin()) {
            Page<Employee> page = employeeRepository.findAll(pageable);
            if (page.isEmpty()) return page.map(e -> EmployeeDetailDto.fromEntity(e, List.of()));

            List<Employee> employees = page.getContent();
            List<DispatchStatus> activeOfPage = dispatchStatusRepository
                    .findByEmployeeInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            employees, "N", today, today
                    );

            final Map<Long, List<EmployeeDispatchDto>> activeMap = activeOfPage.stream()
                    .collect(Collectors.groupingBy(
                            ds -> ds.getEmployee().getId(),
                            Collectors.mapping(EmployeeDispatchDto::fromEntity, Collectors.toList())
                    ));

            return page.map(e -> EmployeeDetailDto.fromEntity(e, activeMap.getOrDefault(e.getId(), List.of())));
        }

        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            List<Branch> myBranches = activeBranchesOf(actor, today);
            if (myBranches.isEmpty()) return Page.empty(pageable);

            List<DispatchStatus> activeInMyBranches = dispatchStatusRepository
                    .findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            myBranches, "N", today, today
                    );

            // distinct + order 유지
            List<Employee> distinctEmployees = activeInMyBranches.stream()
                    .map(DispatchStatus::getEmployee)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(Employee::getId, e -> e, (a, b) -> a, LinkedHashMap::new),
                            m -> new ArrayList<>(m.values())
                    ));

            if (distinctEmployees.isEmpty()) return Page.empty(pageable);

            int from = (int) pageable.getOffset();
            int to = Math.min(from + pageable.getPageSize(), distinctEmployees.size());
            if (from >= to) return new PageImpl<>(List.of(), pageable, distinctEmployees.size());

            List<Employee> pageEmployees = distinctEmployees.subList(from, to);

            final Map<Long, List<EmployeeDispatchDto>> activeMap = activeInMyBranches.stream()
                    .collect(Collectors.groupingBy(
                            ds -> ds.getEmployee().getId(),
                            Collectors.mapping(EmployeeDispatchDto::fromEntity, Collectors.toList())
                    ));

            List<EmployeeDetailDto> content = pageEmployees.stream()
                    .map(e -> EmployeeDetailDto.fromEntity(e, activeMap.getOrDefault(e.getId(), List.of())))
                    .toList();

            return new PageImpl<>(content, pageable, distinctEmployees.size());
        }

        throw new AccessDeniedException("권한이 없습니다.");
    }

    // 지점별 소속 직원 목록 조회 (페이지네이션, 정렬 지원)
    public Page<EmployeeDetailDto> listByBranch(Long branchId, Pageable pageable) {
        // 지점 존재 여부 확인
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();

        // 해당 지점의 활성 배치 목록 조회
        List<DispatchStatus> activeDispatches = dispatchStatusRepository
                .findActiveDispatchesByBranchId(branchId, today);

        if (activeDispatches.isEmpty()) {
            return Page.empty(pageable);
        }

        // 직원 목록 추출 (중복 제거)
        List<Employee> distinctEmployees = activeDispatches.stream()
                .map(DispatchStatus::getEmployee)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Employee::getId, e -> e, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())
                ));

        // 정렬 처리 (고용 상태, 고용 유형, 성별)
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<Employee> comparator = null;
            for (Sort.Order order : sort) {
                Comparator<Employee> currentComparator = getEmployeeComparator(order);
                if (comparator == null) {
                    comparator = currentComparator;
                } else {
                    comparator = comparator.thenComparing(currentComparator);
                }
            }
            if (comparator != null) {
                distinctEmployees.sort(comparator);
            }
        }

        // 페이지네이션 처리
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), distinctEmployees.size());
        if (from >= to) {
            return new PageImpl<>(List.of(), pageable, distinctEmployees.size());
        }

        List<Employee> pageEmployees = distinctEmployees.subList(from, to);

        // 배치 정보 매핑
        final Map<Long, List<EmployeeDispatchDto>> activeMap = activeDispatches.stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getEmployee().getId(),
                        Collectors.mapping(EmployeeDispatchDto::fromEntity, Collectors.toList())
                ));

        // DTO 변환
        List<EmployeeDetailDto> content = pageEmployees.stream()
                .map(e -> EmployeeDetailDto.fromEntity(e, activeMap.getOrDefault(e.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, pageable, distinctEmployees.size());
    }

    // 정렬을 위한 Comparator 생성
    private Comparator<Employee> getEmployeeComparator(Sort.Order order) {
        String property = order.getProperty();
        boolean ascending = order.getDirection().isAscending();

        Comparator<Employee> comparator = switch (property) {
            case "employmentStatus" -> Comparator.comparing(
                    e -> e.getEmploymentStatus() != null ? e.getEmploymentStatus().name() : "",
                    Comparator.nullsLast(String::compareTo)
            );
            case "employmentType" -> Comparator.comparing(
                    e -> e.getEmploymentType() != null ? e.getEmploymentType().name() : "",
                    Comparator.nullsLast(String::compareTo)
            );
            case "gender" -> Comparator.comparing(
                    e -> e.getGender() != null ? e.getGender().name() : "",
                    Comparator.nullsLast(String::compareTo)
            );
            case "name" -> Comparator.comparing(
                    Employee::getName,
                    Comparator.nullsLast(String::compareTo)
            );
            case "hireDate" -> Comparator.comparing(
                    Employee::getHireDate,
                    Comparator.nullsLast(LocalDate::compareTo)
            );
            default -> Comparator.comparing(Employee::getId);
        };

        return ascending ? comparator : comparator.reversed();
    }

    // 마이페이지
    public EmployeeDetailDto getMyPage() {
        Auth auth = readAuth();
        Employee me = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));
        return buildDetail(me);
    }

    private EmployeeDetailDto buildDetail(Employee target) {
        List<EmployeeDispatchDto> history = dispatchStatusRepository
                .findAllByEmployeeOrderByAssignedFromDesc(target)
                .stream()
                .map(EmployeeDispatchDto::fromEntity)
                .toList();
        return EmployeeDetailDto.fromEntity(target, history);
    }

    private List<Branch> activeBranchesOf(Employee employee, LocalDate today) {
        return dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employee, "N", today, today
                ).stream()
                .map(DispatchStatus::getBranch)
                .distinct()
                .toList();
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
        String role = String.valueOf(claims.get("role"));
        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    private record Auth(Long employeeId, String role) {
        public boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        public boolean isBranchOrFranchiseAdmin() {
            return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role);
        }
    }
}
