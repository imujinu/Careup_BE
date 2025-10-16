package com.careup.ordering.domain.payment.repository;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByOrder_Member_Id(Long memberId);

    // 주문으로 결제 조회
    Optional<Payment> findByOrder(Order order);
    
    // paymentKey로 결제 조회
    Optional<Payment> findByPaymentKey(String paymentKey);
}
