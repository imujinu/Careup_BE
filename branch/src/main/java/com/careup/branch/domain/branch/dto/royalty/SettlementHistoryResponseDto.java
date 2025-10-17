package com.careup.branch.domain.branch.dto.royalty;

import com.careup.branch.domain.branch.entity.CalculationMethod;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementHistoryResponseDto {
    private Long royaltyId;
    private String applicableMonth;
    private CalculationMethod calculationMethod;
    private BigDecimal percentage;
    private Long fixedAmount;
    private Long amount;
    private LocalDate dueDate;
    private SettlementStatus settlementStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}

