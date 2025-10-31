package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Documents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentsRepository extends JpaRepository<Documents, Long> {
    // 특정 지점의 서류 조회 (Branch 직접 참조) - 페이지네이션
    Page<Documents> findByBranch_Id(Long branchId, Pageable pageable);

    // 특정 지점에 배치된 직원들의 서류 조회 (페이지네이션) - 기존 메서드 유지
    Page<Documents> findByEmployee_DispatchStatuses_Branch_Id(Long branchId, Pageable pageable);
}
