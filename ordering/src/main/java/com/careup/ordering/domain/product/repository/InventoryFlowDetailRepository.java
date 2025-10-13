package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryFlowDetailRepository extends JpaRepository<InventoryFlowDetail, Long> {
    
    // 지점 상품별 재고 이력 조회 (최신순)
    List<InventoryFlowDetail> findByBranchProductIdOrderByCreateAtDesc(Long branchProductId);
}
