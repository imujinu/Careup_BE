package com.careup.ordering.domain.order.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 재고 알림 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDto {
    private Long productId;
    private String productName;
    private Long currentStock;
    private Long safetyStock;
    private String alertLevel;  // "CRITICAL", "WARNING"
}

