package com.careup.branch.domain.branch.dto.royalty;

import com.careup.branch.domain.branch.entity.CalculationMethod;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoyaltyListResponseDto {
    private Long royaltyId;
    private Long branchId;
    private String branchName;
    private String applicableMonth;
    private CalculationMethod calculationMethod;
    private BigDecimal percentage;
    private Long fixedAmount;
    private Long amount;
    private LocalDate dueDate;
    private SettlementStatus settlementStatus;
}

