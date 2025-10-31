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
import org.springframework.security.core.GrantedAuthority;
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

        // HQ_ADMIN: 전 직원 페이지네이션 + 현재 배치 지점 매핑
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

        // 지점/가맹 관리자: 내가 관리 중인 지점의 현재 배치 직원만
        if (auth.isBranchOrFranchiseAdmin()) {
            Employee actor = employeeRepository.findById(auth.employeeId())
                    .orElseThrow(() -> new EntityNotFoundException("권한을 확인할 수 없습니다."));

            List<Branch> myBranches = activeBranchesOf(actor, today);
            if (myBranches.isEmpty()) return Page.empty(pageable);

            List<DispatchStatus> activeInMyBranches = dispatchStatusRepository
                    .findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            myBranches, "N", today, today
                    );

            // distinct + 순서 유지
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
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();

        List<DispatchStatus> activeDispatches = dispatchStatusRepository
                .findActiveDispatchesByBranchId(branch.getId(), today);

        if (activeDispatches.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Employee> distinctEmployees = activeDispatches.stream()
                .map(DispatchStatus::getEmployee)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Employee::getId, e -> e, (a, b) -> a, LinkedHashMap::new),
                        m -> new ArrayList<>(m.values())
                ));

        // 정렬 처리
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<Employee> comparator = null;
            for (Sort.Order order : sort) {
                Comparator<Employee> currentComparator = getEmployeeComparator(order);
                comparator = (comparator == null) ? currentComparator : comparator.thenComparing(currentComparator);
            }
            if (comparator != null) distinctEmployees.sort(comparator);
        }

        // 페이지네이션
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), distinctEmployees.size());
        if (from >= to) return new PageImpl<>(List.of(), pageable, distinctEmployees.size());

        List<Employee> pageEmployees = distinctEmployees.subList(from, to);

        final Map<Long, List<EmployeeDispatchDto>> activeMap = activeDispatches.stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getEmployee().getId(),
                        Collectors.mapping(EmployeeDispatchDto::fromEntity, Collectors.toList())
                ));

        List<EmployeeDetailDto> content = pageEmployees.stream()
                .map(e -> EmployeeDetailDto.fromEntity(e, activeMap.getOrDefault(e.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, pageable, distinctEmployees.size());
    }

    // 정렬 Comparator
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

    /**
     * 내부 API용: employeeId로 현재 활성화된 지점 ID 조회
     * 여러 지점에 배치된 경우 첫 번째 지점을 반환
     */
    public Long getBranchIdByEmployeeId(Long employeeId) {
        LocalDate today = LocalDate.now();
        
        Optional<DispatchStatus> activeDispatch = dispatchStatusRepository
                .findActiveDispatchByEmployeeId(employeeId, today);
        
        if (activeDispatch.isPresent()) {
            return activeDispatch.get().getBranch().getId();
        }
        
        // 활성화된 배치가 없는 경우 null 반환
        return null;
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

    /**
     * 인증 정보에서 ROLE_ 접두어를 안전하게 제거하여 일관된 역할 문자열(HQ_ADMIN 등)로 반환
     */
    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        // 1) JwtTokenFilter가 Claims를 details에 넣어둔 경우
        Object details = authentication.getDetails();
        if (details instanceof Claims claims) {
            Long employeeId = claims.get("employeeId", Long.class);
            String rawRole = String.valueOf(claims.get("role"));
            String role = normalizeRole(rawRole);
            if (employeeId == null || role == null) {
                throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
            }
            return new Auth(employeeId, role);
        }

        // 2) 그 외 (Authentication name/authorities 기반)
        Long employeeId = null;
        try { employeeId = Long.valueOf(authentication.getName()); } catch (Exception ignored) {}
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .findFirst()
                .orElse(null);

        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    private String normalizeRole(String raw) {
        if (raw == null) return null;
        return raw.startsWith("ROLE_") ? raw.substring(5) : raw;
    }

    private record Auth(Long employeeId, String role) {
        public boolean isHqAdmin() { return "HQ_ADMIN".equals(role); }
        public boolean isBranchOrFranchiseAdmin() {
            return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role);
        }
    }
}
