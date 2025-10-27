package com.careup.branch.domain.purchaseOrder.repository;

import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderDetailRepository extends JpaRepository<PurchaseOrderDetail, Long> {
    
    // 발주에 따른 상세 내역 조회
    List<PurchaseOrderDetail> findByPurchaseOrder(PurchaseOrder purchaseOrder);
    
    // 통계용 쿼리 메서드
    
    // 기간별 상품별 통계 조회
    @Query("SELECT pod.productId, " +
           "SUM(pod.quantity), " +
           "SUM(pod.approvedQuantity), " +
           "SUM(pod.subtotalPrice), " +
           "COUNT(DISTINCT pod.purchaseOrder.id) " +
           "FROM PurchaseOrderDetail pod " +
           "WHERE pod.purchaseOrder.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pod.productId " +
           "ORDER BY SUM(pod.quantity) DESC")
    List<Object[]> findProductStatistics(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
}

