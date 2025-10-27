package com.careup.ordering.domain.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promotion")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_products_id", nullable = false)
    private BranchProduct branchProduct;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;
    
    // 비즈니스 로직 메서드
    public void updatePromotion(BigDecimal discountRate, LocalDate startDate, LocalDate endDate) {
        if (discountRate != null) {
            this.discountRate = discountRate;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
    }
    
    public void activate() {
        this.status = PromotionStatus.ACTIVE;
    }
    
    public void deactivate() {
        this.status = PromotionStatus.INACTIVE;
    }
}