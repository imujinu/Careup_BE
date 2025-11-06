package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryFlowDetailRepository extends JpaRepository<InventoryFlowDetail, Long> {
    
    // 지점별 입출고 기록 조회 (속성 정보 포함)
    @Query("SELECT ifd FROM InventoryFlowDetail ifd " +
           "JOIN FETCH ifd.branchProduct bp " +
           "JOIN FETCH bp.product p " +
           "LEFT JOIN FETCH bp.attributeValue av " +
           "LEFT JOIN FETCH av.attributeType " +
           "WHERE bp.branchId = :branchId")
    List<InventoryFlowDetail> findByBranchId(@Param("branchId") Long branchId);

    // 지점&상품별 입출고 기록 조회 (속성 정보 포함)
    @Query("SELECT ifd FROM InventoryFlowDetail ifd " +
           "JOIN FETCH ifd.branchProduct bp " +
           "JOIN FETCH bp.product p " +
           "LEFT JOIN FETCH bp.attributeValue av " +
           "LEFT JOIN FETCH av.attributeType " +
           "WHERE bp.branchId = :branchId AND p.id = :productId")
    List<InventoryFlowDetail> findByBranchIdAndProductId(@Param("branchId") Long branchId, @Param("productId") Long productId);

    // 지점&비고로 입출고 기록 조회 (속성 정보 포함)
    @Query("SELECT ifd FROM InventoryFlowDetail ifd " +
           "JOIN FETCH ifd.branchProduct bp " +
           "JOIN FETCH bp.product p " +
           "LEFT JOIN FETCH bp.attributeValue av " +
           "LEFT JOIN FETCH av.attributeType " +
           "WHERE bp.branchId = :branchId AND ifd.remark LIKE %:reason%")
    List<InventoryFlowDetail> findByBranchIdAndRemarkContaining(@Param("branchId") Long branchId, @Param("reason") String reason);

    // 비고로 입출고 기록 조회
    List<InventoryFlowDetail> findByRemarkContaining(String reason);
}
