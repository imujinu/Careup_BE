package com.careup.branch.domain.purchaseOrder.repository;

import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    
    // 특정 지점의 발주 목록 조회
    List<PurchaseOrder> findByBranchId(Long branchId);
    
    // 기간별 발주 조회
    List<PurchaseOrder> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 특정 지점의 기간별 발주 조회
    List<PurchaseOrder> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
}

