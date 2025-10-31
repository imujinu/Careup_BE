package com.careup.branch.domain.branch.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 카테고리별 매출 분석 카드 (재고 분석)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySalesCardDto {
    private Map<String, Long> categorySalesDistribution;  // 카테고리별 매출 비중 (차트용)
    private Long totalSales;           // 총 매출
    private String topCategory;        // 최고 카테고리
    private Long topCategorySales;     // 최고 카테고리 매출
}

