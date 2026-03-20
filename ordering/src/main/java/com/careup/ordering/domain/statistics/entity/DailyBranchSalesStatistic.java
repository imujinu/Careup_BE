package com.careup.ordering.domain.statistics.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_branch_sales_statistics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_branch_sales_date", columnNames = {"branch_id", "sales_date"})
        },
        indexes = {
                @Index(name = "idx_sales_date_branch", columnList = "sales_date, branch_id")
        }
)
public class DailyBranchSalesStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statistic_id")
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(name = "total_sales_amount", nullable = false)
    private Long totalSalesAmount;

    @Column(name = "total_order_count", nullable = false)
    private Long totalOrderCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public DailyBranchSalesStatistic(Long branchId, LocalDate salesDate, Long totalSalesAmount, Long totalOrderCount) {
        this.branchId = branchId;
        this.salesDate = salesDate;
        this.totalSalesAmount = totalSalesAmount;
        this.totalOrderCount = totalOrderCount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 동일 지점/날짜의 데이터를 병합(누적)할 때 사용하는 메서드
    public void addStatistics(Long additionalAmount, Long additionalCount) {
        this.totalSalesAmount += additionalAmount;
        this.totalOrderCount += additionalCount;
        this.updatedAt = LocalDateTime.now();
    }
}