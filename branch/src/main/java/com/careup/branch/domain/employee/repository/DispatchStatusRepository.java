package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
