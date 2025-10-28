package com.careup.branch.domain.branch.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesForecast extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    //예상매출액 1:N 지점
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="branch_id", nullable = false)
    private Branch branch;

    //금액
    @Column(nullable = false)
    private Long amount;

    //시작 기간
    @Column(name = "period_start", nullable = false)
    private Date periodStart;

    //종료 기간
    @Column(name="period_end", nullable = false)
    private Date period_end;

    /**
     * 예상 매출액 업데이트
     */
    public void updateAmount(Long newAmount) {
        this.amount = newAmount;
    }

    /**
     * 예상 매출 기간 업데이트
     */
    public void updatePeriod(Date periodStart, Date periodEnd) {
        this.periodStart = periodStart;
        this.period_end = periodEnd;
    }
}
