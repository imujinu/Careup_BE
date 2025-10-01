package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.DispatchStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DispatchStatusRepository extends JpaRepository<DispatchStatus, Long> {

    @EntityGraph(attributePaths = {"branch"})
    Optional<DispatchStatus> findFirstByEmployeeIdAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
            Long employeeId, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findAllByEmployeeIdOrderByAssignedFromDesc(Long employeeId);

    @EntityGraph(attributePaths = {"branch"})
    List<DispatchStatus> findByEmployeeIdInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
            List<Long> employeeIds, String placementYn, LocalDate fromInclusive, LocalDate toInclusive
    );

    boolean existsByEmployeeIdAndBranchId(Long employeeId, Long branchId);

    void deleteByEmployeeId(Long employeeId);

    @Query("select distinct d.branch.id from DispatchStatus d " +
            "where d.employee.id = :employeeId and d.assignedFrom <= :today and d.assignedTo >= :today")
    List<Long> findManageableBranchIds(Long employeeId, LocalDate today);

    @Query("select count(d) from DispatchStatus d " +
            "where d.employee.id = :targetEmployeeId " +
            "and d.branch.id in :branchIds " +
            "and d.assignedFrom <= :today and d.assignedTo >= :today")
    long countActiveInBranches(Long targetEmployeeId, List<Long> branchIds, LocalDate today);
}
