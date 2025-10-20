package com.careup.branch.domain.owner.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.owner.dto.request.OwnerAssignRequestDto;
import com.careup.branch.domain.owner.dto.request.OwnerUpdateRequestDto;
import com.careup.branch.domain.owner.dto.response.OwnerResponseDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final Clock clock;

    /**
     * 점주 등록 및 할당 (직원 -> 지점 관리자로 권한 승격)
     */
    @Transactional
    public OwnerResponseDto assignOwner(OwnerAssignRequestDto request) {
        // 1. 직원 조회
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        // 2. 지점 조회
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        // 3. 권한 타입 검증 (BRANCH_ADMIN 또는 FRANCHISE_OWNER만 허용)
        AuthorityType targetAuthority = request.getAuthorityType();
        if (targetAuthority != AuthorityType.BRANCH_ADMIN && targetAuthority != AuthorityType.FRANCHISE_OWNER) {
            throw new IllegalArgumentException("점주 권한은 BRANCH_ADMIN 또는 FRANCHISE_OWNER만 가능합니다.");
        }

        // 4. 현재 직원이 이미 관리자 권한을 가지고 있는지 확인
        if (employee.getAuthorityType() == AuthorityType.HQ_ADMIN) {
            throw new IllegalArgumentException("본사 관리자는 점주로 할당할 수 없습니다.");
        }

        // 5. 해당 지점에 이미 점주가 존재하는지 확인
        LocalDate today = LocalDate.now(clock);
        List<DispatchStatus> existingOwners = dispatchStatusRepository
                .findByBranchAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        branch, "N", today, today
                );

        for (DispatchStatus ds : existingOwners) {
            Employee existingEmployee = ds.getEmployee();
            AuthorityType existingAuth = existingEmployee.getAuthorityType();

            if ((existingAuth == AuthorityType.BRANCH_ADMIN || existingAuth == AuthorityType.FRANCHISE_OWNER)
                    && !existingEmployee.getId().equals(employee.getId())) {
                throw new IllegalArgumentException("해당 지점에 이미 점주가 존재합니다.");
            }
        }

        // 6. 직원의 권한 타입을 지점 관리자로 변경
        updateEmployeeAuthority(employee, targetAuthority);

        // 7. 해당 직원의 기존 배치 정보 종료 처리 (오늘 날짜 이전으로)
        LocalDate yesterday = today.minusDays(1);
        List<DispatchStatus> activeDispatches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employee, "N", today, today
                );

        for (DispatchStatus ds : activeDispatches) {
            if (ds.getAssignedTo().isAfter(yesterday)) {
                updateDispatchEndDate(ds, yesterday);
            }
        }

        // 8. 새로운 배치 정보 생성 (해당 지점에 점주로 배치)
        LocalDate endDate = LocalDate.of(9999, 12, 31); // 무기한
        DispatchStatus newDispatch = DispatchStatus.builder()
                .employee(employee)
                .branch(branch)
                .assignedFrom(today)
                .assignedTo(endDate)
                .placementYn("N")
                .build();

        dispatchStatusRepository.save(newDispatch);

        return OwnerResponseDto.fromEntity(employee, branch.getId(), branch.getName());
    }

    /**
     * 점주 정보 수정 (권한 타입 또는 관리 지점 변경)
     */
    @Transactional
    public OwnerResponseDto updateOwner(Long employeeId, OwnerUpdateRequestDto request) {
        // 1. 직원 조회
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        // 2. 지점 조회
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        // 3. 점주 권한 확인
        if (employee.getAuthorityType() != AuthorityType.BRANCH_ADMIN
                && employee.getAuthorityType() != AuthorityType.FRANCHISE_OWNER) {
            throw new IllegalArgumentException("해당 직원은 점주가 아닙니다.");
        }

        // 4. 권한 타입 검증
        AuthorityType targetAuthority = request.getAuthorityType();
        if (targetAuthority != AuthorityType.BRANCH_ADMIN && targetAuthority != AuthorityType.FRANCHISE_OWNER) {
            throw new IllegalArgumentException("점주 권한은 BRANCH_ADMIN 또는 FRANCHISE_OWNER만 가능합니다.");
        }

        // 5. 새로운 지점에 이미 점주가 있는지 확인 (지점 변경 시)
        LocalDate today = LocalDate.now(clock);
        List<DispatchStatus> currentDispatches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employee, "N", today, today
                );

        Long currentBranchId = currentDispatches.isEmpty() ? null : currentDispatches.get(0).getBranch().getId();

        if (!branch.getId().equals(currentBranchId)) {
            // 지점 변경 시, 새 지점에 점주가 있는지 확인
            List<DispatchStatus> existingOwners = dispatchStatusRepository
                    .findByBranchAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                            branch, "N", today, today
                    );

            for (DispatchStatus ds : existingOwners) {
                Employee existingEmployee = ds.getEmployee();
                AuthorityType existingAuth = existingEmployee.getAuthorityType();

                if ((existingAuth == AuthorityType.BRANCH_ADMIN || existingAuth == AuthorityType.FRANCHISE_OWNER)
                        && !existingEmployee.getId().equals(employee.getId())) {
                    throw new IllegalArgumentException("새로운 지점에 이미 점주가 존재합니다.");
                }
            }

            // 기존 배치 종료
            LocalDate yesterday = today.minusDays(1);
            for (DispatchStatus ds : currentDispatches) {
                updateDispatchEndDate(ds, yesterday);
            }

            // 새로운 배치 생성
            LocalDate endDate = LocalDate.of(9999, 12, 31);
            DispatchStatus newDispatch = DispatchStatus.builder()
                    .employee(employee)
                    .branch(branch)
                    .assignedFrom(today)
                    .assignedTo(endDate)
                    .placementYn("N")
                    .build();

            dispatchStatusRepository.save(newDispatch);
        }

        // 6. 권한 타입 업데이트
        updateEmployeeAuthority(employee, targetAuthority);

        return OwnerResponseDto.fromEntity(employee, branch.getId(), branch.getName());
    }

    /**
     * 점주 삭제 (권한 해제: 지점 관리자 -> 일반 직원으로 변경)
     */
    @Transactional
    public void removeOwner(Long employeeId) {
        // 1. 직원 조회
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        // 2. 점주 권한 확인
        if (employee.getAuthorityType() != AuthorityType.BRANCH_ADMIN
                && employee.getAuthorityType() != AuthorityType.FRANCHISE_OWNER) {
            throw new IllegalArgumentException("해당 직원은 점주가 아닙니다.");
        }

        // 3. 권한을 일반 직원(STAFF)으로 변경
        updateEmployeeAuthority(employee, AuthorityType.STAFF);

        // 4. 현재 배치 정보 종료 처리
        LocalDate today = LocalDate.now(clock);
        LocalDate yesterday = today.minusDays(1);

        List<DispatchStatus> activeDispatches = dispatchStatusRepository
                .findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        employee, "N", today, today
                );

        for (DispatchStatus ds : activeDispatches) {
            updateDispatchEndDate(ds, yesterday);
        }
    }

    /**
     * 특정 지점의 점주 조회
     */
    public OwnerResponseDto getOwnerByBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("지점을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now(clock);
        List<DispatchStatus> dispatches = dispatchStatusRepository
                .findByBranchAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        branch, "N", today, today
                );

        for (DispatchStatus ds : dispatches) {
            Employee employee = ds.getEmployee();
            AuthorityType auth = employee.getAuthorityType();

            if (auth == AuthorityType.BRANCH_ADMIN || auth == AuthorityType.FRANCHISE_OWNER) {
                return OwnerResponseDto.fromEntity(employee, branch.getId(), branch.getName());
            }
        }

        throw new EntityNotFoundException("해당 지점에 점주가 존재하지 않습니다.");
    }

    /**
     * 모든 점주 목록 조회
     */
    public List<OwnerResponseDto> getAllOwners() {
        LocalDate today = LocalDate.now(clock);

        List<DispatchStatus> allDispatches = dispatchStatusRepository
                .findByPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        "N", today, today
                );

        return allDispatches.stream()
                .filter(ds -> {
                    AuthorityType auth = ds.getEmployee().getAuthorityType();
                    return auth == AuthorityType.BRANCH_ADMIN || auth == AuthorityType.FRANCHISE_OWNER;
                })
                .map(ds -> OwnerResponseDto.fromEntity(
                        ds.getEmployee(),
                        ds.getBranch().getId(),
                        ds.getBranch().getName()
                ))
                .toList();
    }

    // ===== Private Helper Methods =====

    private void updateEmployeeAuthority(Employee employee, AuthorityType newAuthority) {
        employee.changeAuthorityType(newAuthority);
    }

    private void updateDispatchEndDate(DispatchStatus dispatch, LocalDate newEndDate) {
        dispatch.endAssignment(newEndDate);
    }
}
