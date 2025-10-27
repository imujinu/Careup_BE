package com.careup.ordering.domain.member.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyal_customer")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyalCustomer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyal_customer_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false)
    private LoyalGrade grade;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;


    // 비즈니스 로직 메서드
    public void updateAmount(BigDecimal amount) {
        this.totalAmount = this.totalAmount.add(amount);
        this.orderCount++;
        updateGrade();
    }

    public void updateInfo(BigDecimal totalAmount, Integer orderCount, LoyalGrade grade) {
        if (totalAmount != null) {
            this.totalAmount = totalAmount;
        }
        if (orderCount != null) {
            this.orderCount = orderCount;
        }
        if (grade != null) {
            this.grade = grade;
        } else {
            updateGrade();
        }
    }

    private void updateGrade() {
        if (this.totalAmount.compareTo(new BigDecimal("1000000")) >= 0) {
            this.grade = LoyalGrade.VIP;
        } else if (this.totalAmount.compareTo(new BigDecimal("500000")) >= 0) {
            this.grade = LoyalGrade.GOLD;
        } else if (this.totalAmount.compareTo(new BigDecimal("100000")) >= 0) {
            this.grade = LoyalGrade.SILVER;
        } else {
            this.grade = LoyalGrade.BRONZE;
        }
    }
}
