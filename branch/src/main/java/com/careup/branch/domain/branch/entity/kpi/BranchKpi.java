package com.careup.branch.domain.branch.entity.kpi;

import com.careup.branch.domain.branch.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 지점별 KPI 엔티티
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public class BranchKpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KPI ID (kpi FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kpi_id")
    private KPI kpiId;

    // 지점 ID (branch FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branchId;

    // 목표값
    @Column(nullable = false)
    private BigDecimal targetValue;

    // 현재값
    @Column(nullable = false)
    private BigDecimal currentValue;

    // 달성률
    @Column(nullable = false)
    private Double achievementRate;

    // 목표 기간 시작일
    @Column(nullable = false)
    private LocalDate startDate;

    // 목표 기간 종료일
    @Column(nullable = false)
    private LocalDate endDate;

    // KPI 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KpiStatus kpiStatus;

    // 업데이트 메서드
    public void updateBranchKpi(KPI kpi, Branch branch, BigDecimal targetValue,
                               BigDecimal currentValue, Double achievementRate,
                               LocalDate startDate, LocalDate endDate, KpiStatus kpiStatus) {
        this.kpiId = kpi;
        this.branchId = branch;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.achievementRate = achievementRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.kpiStatus = kpiStatus;
    }
}
