package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DispatchStatusRepository extends JpaRepository<DispatchStatus, Long> {

    /// 직원의 모든 배치 이력(브랜치 함께 로딩)
    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findAllByEmployeeOrderByAssignedFromDesc(Employee employee);

    /// 특정 직원의 오늘(active, placementYn='N') 배치 전체
    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Employee employee, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    /// 특정 직원의 오늘(active, placementYn='N') 배치 중 가장 최근(시작일 내림차순 1건)
    @EntityGraph(attributePaths = {"branch"})
    Optional<DispatchStatus> findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
            Employee employee, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    /// 지점 컬렉션 내의 오늘(active, placementYn='N') 배치(직원/직급, 지점 함께 로딩)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Branch> branches, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    /// 직원 컬렉션의 오늘(active, placementYn='N') 배치(직원/직급, 지점 함께 로딩)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByEmployeeInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Employee> employees, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    /// 권한 검사: 대상 직원이 내 지점 컬렉션에 오늘(active, placementYn='N') 포함되는지
    boolean existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Employee employee, Collection<Branch> branches, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    /// 업데이트 시 기존 배치 전량 삭제
    void deleteByEmployee(Employee employee);
}
