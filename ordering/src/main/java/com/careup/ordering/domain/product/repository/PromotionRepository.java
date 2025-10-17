package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Promotion;
import com.careup.ordering.domain.product.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    /**
     * 지점 상품별 프로모션 조회
     */
    List<Promotion> findByBranchProductId(Long branchProductId);
    
    /**
     * 상태별 프로모션 조회
     */
    List<Promotion> findByStatus(PromotionStatus status);
    
    /**
     * 특정 지점 상품의 활성 프로모션 조회
     */
    List<Promotion> findByBranchProductIdAndStatus(Long branchProductId, PromotionStatus status);
}
