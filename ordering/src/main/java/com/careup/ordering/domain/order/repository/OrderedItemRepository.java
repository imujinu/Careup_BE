package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.OrderedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderedItemRepository extends JpaRepository<OrderedItem, Long> {

    // 주문별 상품 조회
    List<OrderedItem> findByOrderId(Long orderId);

    // 특정 지점의 상품별 판매 통계 조회
    @Query("SELECT oi.branchProduct.product.id as productId, oi.branchProduct.product.name as productName, " +
           "SUM(oi.quantity) as totalQuantity, SUM(oi.totalPrice) as totalSales, " +
           "oi.branchProduct.product.supplyPrice as supplyPrice, AVG(oi.unitPrice) as avgSellingPrice, " +
           "COUNT(DISTINCT oi.order.id) as orderCount " +
           "FROM OrderedItem oi " +
           "WHERE oi.order.branchId = :branchId " +
           "AND oi.order.orderStatus = com.careup.ordering.domain.order.entity.OrderStatus.CONFIRMED " +
           "AND oi.order.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY oi.branchProduct.product.id, oi.branchProduct.product.name, oi.branchProduct.product.supplyPrice " +
           "ORDER BY totalSales DESC")
    List<Object[]> findProductSalesStatistics(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 본점(HQ)용: 전체 지점 대상 상품별 판매 통계 조회 (기간 내 확정 주문 기준)
    @Query("SELECT oi.branchProduct.product.id as productId, oi.branchProduct.product.name as productName, " +
           "SUM(oi.quantity) as totalQuantity, SUM(oi.totalPrice) as totalSales, " +
           "oi.branchProduct.product.supplyPrice as supplyPrice, AVG(oi.unitPrice) as avgSellingPrice, " +
           "COUNT(DISTINCT oi.order.id) as orderCount " +
           "FROM OrderedItem oi " +
           "WHERE oi.order.orderStatus = com.careup.ordering.domain.order.entity.OrderStatus.CONFIRMED " +
           "AND oi.order.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY oi.branchProduct.product.id, oi.branchProduct.product.name, oi.branchProduct.product.supplyPrice")
    List<Object[]> findHqProductSalesStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
