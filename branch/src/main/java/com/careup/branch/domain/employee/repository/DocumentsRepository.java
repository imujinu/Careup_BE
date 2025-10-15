package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Documents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentsRepository extends JpaRepository<Documents, Long> {
    // 특정 지점 ID로 필터링하여 페이지 조회
    Page<Documents> findByEmployee_Id(Long employeeId, Pageable pageable);

    // 특정 지점에 속한 단건 조회
    Optional<Documents> findByIdAndEmployee_Id(Long id, Long employeeId);
}
