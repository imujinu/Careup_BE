package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchUpdateRequestRepository extends JpaRepository<BranchUpdateRequest, Long> {
    // 특정 지점에 대해 이미 처리 대기 중인 요청이 있는지 확인하기 위한 메서드
    Optional<BranchUpdateRequest> findByBranchAndStatus(Branch branch, BranchUpdateRequest.RequestStatus status);

    // 모든 수정 요청 조회 (페이징)
    Page<BranchUpdateRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 특정 상태의 수정 요청 조회 (페이징)
    Page<BranchUpdateRequest> findByStatusOrderByCreatedAtDesc(BranchUpdateRequest.RequestStatus status, Pageable pageable);
}
