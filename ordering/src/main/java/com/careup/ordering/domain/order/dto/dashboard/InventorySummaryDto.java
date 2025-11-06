package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 현황 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDto {
    private Long totalProducts;        // 총 재고 품목
    private Long lowStockProducts;     // 재고 부족 품목
    private Double stockFulfillmentRate;  // 재고 충족률 (%)
    private List<StockAlertDto> stockAlerts;  // 재고 알림
}

