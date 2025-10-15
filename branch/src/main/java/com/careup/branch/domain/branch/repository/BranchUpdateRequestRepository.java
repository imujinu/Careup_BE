package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchUpdateRequestRepository extends JpaRepository<BranchUpdateRequest, Long> {
    // 특정 지점에 대해 이미 처리 대기 중인 요청이 있는지 확인하기 위한 메서드
    Optional<BranchUpdateRequest> findByBranchAndStatus(Branch branch, BranchUpdateRequest.RequestStatus status);
}
