package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.OrderedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Objects;

public interface OrderedItemRepository extends JpaRepository<OrderedItem,Long> {
    // 주문별 상품 조회
    List<OrderedItem> findByOrderId(Long orderId);

    // 상품별 판매 통계
    @Query("SELECT oi.productId, SUM(oi.quantity) as totalQty " +
            "FROM OrderedItem oi GROUP BY oi.productId ORDER BY totalQty DESC")
    List<Object[]> findProductSalesStatistics();
}
