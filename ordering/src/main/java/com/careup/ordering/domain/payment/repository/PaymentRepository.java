package com.careup.ordering.domain.payment.repository;

import com.careup.ordering.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment,Long> {

}
