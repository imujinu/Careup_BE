package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DispatchStatusRepository extends JpaRepository<DispatchStatus, Long> {

    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findAllByEmployeeOrderByAssignedFromDesc(Employee employee);

    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Employee employee, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    @EntityGraph(attributePaths = {"branch"})
    Optional<DispatchStatus> findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
            Employee employee, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Branch> branches, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByEmployeeInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Employee> employees, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    boolean existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Employee employee, Collection<Branch> branches, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByEmployee(Employee employee);

    @EntityGraph(attributePaths = {"employee", "employee.jobGrade"})
    @Query("""
           select distinct ds.employee
             from DispatchStatus ds
            where ds.placementYn = 'N'
              and ds.branch.id in :branchIds
              and ds.assignedFrom <= :to
              and ds.assignedTo   >= :from
           """)
    List<Employee> findActiveEmployeesByBranchesAndRange(@Param("branchIds") Collection<Long> branchIds,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to);

    /**
     * 직원 ID와 현재 날짜를 기준으로 유효한(배치중인) 지점 정보 조회
     */
    @Query("select ds from DispatchStatus ds " +
        "where ds.employee.id = :employeeId " +
        "and ds.placementYn = 'N' " +
        "and :currentDate between ds.assignedFrom and ds.assignedTo")
    Optional<DispatchStatus> findActiveDispatchByEmployeeId(
            @Param("employeeId") Long employeeId,
            @Param("currentDate") LocalDate currentDate
    );
}
