package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // placementYn 조건 없이 기간만으로 조회 (점주 조회용 대체 메서드)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByBranchInAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Branch> branches, LocalDate fromInclusive, LocalDate toInclusive
    );

    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByEmployeeInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Collection<Employee> employees, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    boolean existsByEmployeeAndBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Employee employee, Collection<Branch> branches, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    // 추가: 직원-지점-일자 단건 검증용 존재여부 체크
    boolean existsByEmployee_IdAndBranch_IdAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Long employeeId, Long branchId, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
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

    @Query("select ds from DispatchStatus ds " +
            "where ds.employee.id = :employeeId " +
            "and ds.placementYn = 'N' " +
            "and :currentDate between ds.assignedFrom and ds.assignedTo")
    Optional<DispatchStatus> findActiveDispatchByEmployeeId(
            @Param("employeeId") Long employeeId,
            @Param("currentDate") LocalDate currentDate
    );

    // 지점별 활성 배치 조회 (점주 관리용)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade"})
    List<DispatchStatus> findByBranchAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            Branch branch, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    // 전체 활성 배치 조회 (점주 목록 조회용)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade", "branch"})
    List<DispatchStatus> findByPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    // 지점별 직원 조회 (페이지네이션 지원)
    @EntityGraph(attributePaths = {"employee", "employee.jobGrade"})
    @Query("""
           select ds from DispatchStatus ds
           where ds.branch.id = :branchId
           and ds.placementYn = 'N'
           and ds.assignedFrom <= :currentDate
           and ds.assignedTo >= :currentDate
           """)
    List<DispatchStatus> findActiveDispatchesByBranchId(
            @Param("branchId") Long branchId,
            @Param("currentDate") LocalDate currentDate
    );


    Optional<DispatchStatus> findByEmployee(Employee employee);


    List<DispatchStatus> findAllByBranchId(Long branchId);

    @Query("SELECT ds FROM DispatchStatus ds JOIN FETCH ds.employee WHERE ds.branch = :branch")
    List<DispatchStatus> findAllByBranch(Branch branch);

}
