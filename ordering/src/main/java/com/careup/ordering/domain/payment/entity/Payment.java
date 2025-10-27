package com.careup.ordering.domain.payment.entity;

import com.careup.ordering.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // 토스페이먼츠에서 발급한 결제 키
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    // 토스페이먼츠 거래 고유 번호
    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 결제 승인
     */
    public void confirm(String paymentKey, String pgTransactionId) {
        this.paymentKey = paymentKey;
        this.pgTransactionId = pgTransactionId;
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 결제 취소
     */
    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.canceledAt = LocalDateTime.now();
    }
}
