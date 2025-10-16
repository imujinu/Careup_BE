package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.OrderedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderedItemRepository extends JpaRepository<OrderedItem, Long> {

    // 주문별 상품 조회
    List<OrderedItem> findByOrderId(Long orderId);

}