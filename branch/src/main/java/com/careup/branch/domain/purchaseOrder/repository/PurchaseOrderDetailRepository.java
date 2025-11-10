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

    // 목록 최적화: 주문 ID별 상세 개수 배치 카운트
    @Query("SELECT d.purchaseOrder.id AS orderId, COUNT(d) AS cnt " +
            "FROM PurchaseOrderDetail d " +
            "WHERE d.purchaseOrder.id IN :orderIds " +
            "GROUP BY d.purchaseOrder.id")
    List<Object[]> countByPurchaseOrderIds(@Param("orderIds") List<Long> orderIds);

    // 통계용: 기간별 상품별 집계
    @Query("SELECT pod.productId, " +
            "       pod.productName, " +
            "       SUM(pod.quantity), " +
            "       SUM(pod.approvedQuantity), " +
            "       SUM(pod.subtotalPrice), " +
            "       COUNT(DISTINCT pod.purchaseOrder.id) " +
            "FROM PurchaseOrderDetail pod " +
            "WHERE pod.purchaseOrder.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY pod.productId, pod.productName " +
            "ORDER BY SUM(pod.quantity) DESC")
    List<Object[]> findProductStatistics(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}