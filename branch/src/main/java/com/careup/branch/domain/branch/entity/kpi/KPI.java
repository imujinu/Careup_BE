package com.careup.branch.domain.branch.entity.kpi;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kpi")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class KPI extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 속성명
    @Column(length = 20, nullable = false)
    private String name;

    // 상세 설명
    @Column(length = 100)
    private String description;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private KpiCategory category;

    // 기간 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    // 계산 공식
    @Column(nullable = false)
    private String calculationFormula;


}
