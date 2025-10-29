package com.careup.branch.domain.branch.entity.kpi;

/**
 * KPI 카테고리 Enum
 */
public enum KpiCategory {
    SALES("매출"),          // 매출 관련 KPI
    ORDER("주문"),          // 주문 관련 KPI
    INVENTORY("재고"),      // 재고 관련 KPI
    ATTENDANCE("출근"),     // 출근 관련 KPI
    REVIEW("리뷰"),         // 리뷰 관련 KPI (미구현)
    CUSTOMER("고객만족"),    // 고객만족 관련 KPI (미구현)
    CUSTOM("커스텀");       // 커스텀 KPI

    private final String description;

    KpiCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

