package com.careup.ordering.domain.order.entity;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_date_branch", columnList = "order_status, created_at")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType = OrderType.ONLINE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 255)
    private String rejectedReason;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancelled_reason", length = 255)
    private String cancelledReason;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderedItem> orderedItems = new ArrayList<>();

//    순한참조 할것 같아서 뺴둠 order<->payment
//    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
//    private Payment payment;

    @Builder
    public Order(Member member, Long branchId, Long totalAmount, OrderType orderType) {
        this.member = member;
        this.branchId = branchId;
        this.totalAmount = totalAmount;
        this.orderType = orderType;
        this.createdAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.PENDING;
    }

    // 비즈니스 로직
    
    /**
     * 토스페이먼츠 API용 orderId 반환
     * 형식: CAREUP_ORDER_{orderId}
     * 토스페이먼츠는 6자 이상 64자 이하의 영문 대소문자, 숫자, 특수문자(-, _)만 허용
     * 예: CAREUP_ORDER_1, CAREUP_ORDER_12345
     */
    public String getTossOrderId() {
        return "CAREUP_ORDER_" + this.id;
    }
    
    public void updateStatus(OrderStatus status) {
        this.orderStatus = status;
    }

    public void updateTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void approve(Long approvedBy) {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 승인할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reason, Long rejectedBy) {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 거부할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.REJECTED;
        this.rejectedReason = reason;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
    }

    public void cancel(String reason, Long cancelledBy) {
        if (this.orderStatus == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("이미 승인된 주문은 취소할 수 없습니다.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledReason = reason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
    }
}