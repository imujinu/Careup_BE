package com.careup.ordering.domain.order.dto.response;

import com.careup.ordering.domain.order.dto.CategorySalesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySalesResponseDto {
    private String periodType; // WEEK, MONTH, YEAR
    private Long totalSales; // 전체 매출액
    private List<CategorySalesDto> categories; // 카테고리별 매출 리스트
}

