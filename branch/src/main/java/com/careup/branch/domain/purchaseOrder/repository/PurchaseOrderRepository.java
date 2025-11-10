package com.careup.branch.domain.purchaseOrder.repository;

import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByBranchId(Long branchId);

    List<PurchaseOrder> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<PurchaseOrder> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.branchId = :branchId AND DATE(po.createdAt) = :orderDate AND po.orderStatus = :orderStatus")
    List<PurchaseOrder> findByBranchIdAndOrderDateAndOrderStatus(@Param("branchId") Long branchId,
                                                                 @Param("orderDate") LocalDate orderDate,
                                                                 @Param("orderStatus") OrderStatus orderStatus);

    @Query("SELECT po.orderStatus, COUNT(po) FROM PurchaseOrder po " +
            "WHERE po.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY po.orderStatus")
    List<Object[]> countByOrderStatusAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT po.orderStatus, SUM(po.price) FROM PurchaseOrder po " +
            "WHERE po.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY po.orderStatus")
    List<Object[]> sumAmountByOrderStatusAndDateRange(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT po.branchId, COUNT(po), SUM(po.price), AVG(po.price), " +
            "SUM(CASE WHEN po.orderStatus IN ('APPROVED', 'PARTIAL', 'SHIPPED', 'COMPLETED') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN po.orderStatus = 'REJECTED' THEN 1 ELSE 0 END) " +
            "FROM PurchaseOrder po " +
            "WHERE po.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY po.branchId " +
            "ORDER BY SUM(po.price) DESC")
    List<Object[]> findBranchStatistics(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(po) > 0 FROM PurchaseOrder po " +
            "JOIN po.orderDetails pod " +
            "WHERE po.branchId = :branchId " +
            "AND pod.productId = :productId " +
            "AND po.orderStatus IN ('PENDING', 'APPROVED', 'PARTIAL', 'SHIPPED') " +
            "AND po.createdAt >= :checkDate")
    boolean existsPendingOrderForBranchAndProduct(@Param("branchId") Long branchId,
                                                  @Param("productId") Long productId,
                                                  @Param("checkDate") LocalDateTime checkDate);

    Page<PurchaseOrder> findByOrderStatusNot(OrderStatus orderStatus, Pageable pageable);

    Page<PurchaseOrder> findByBranchIdAndOrderStatusNot(Long branchId, OrderStatus orderStatus, Pageable pageable);
}