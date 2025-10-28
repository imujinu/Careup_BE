package com.careup.branch.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChatLaborCostDto {
    private Long dailySales;
    private Long laborCost;
    private BigDecimal laborCostRatio;
    private BigDecimal preDayAvg;
    private BigDecimal preWeekAvg;
    private String message;

}
