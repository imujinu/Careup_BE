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
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
    public void updateStatus(OrderStatus status) {
        this.orderStatus = status;
    }

    public void approve(Long approvedBy) {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 승인할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 거부할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.rejectedReason = reason;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("이미 승인된 주문은 취소할 수 없습니다.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }
}