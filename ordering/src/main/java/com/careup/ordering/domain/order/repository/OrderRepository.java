package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원별 주문 조회
    List<Order> findByMemberId(Long memberId);

    // 지점별 주문 조회
    List<Order> findByBranchId(Long branchId);

    // 주문 상태별 조회
    List<Order> findByOrderStatus(OrderStatus status);

    // 기간별 주문 조회
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}