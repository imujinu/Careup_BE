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

/**
 * 로열티 엑셀 변환을 위한 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoyaltyExcelDto {
    // 로열티 기본 정보
    private Long royaltyId;
    private String branchName;
    private String applicableMonth;
    private CalculationMethod calculationMethod;
    private BigDecimal percentage;
    private Long fixedAmount;
    private Long amount;

    // 매출 정보
    private Long totalSales;

    // 납부 정보
    private LocalDate dueDate;
    private SettlementStatus settlementStatus;
    private LocalDateTime paymentDate;

    // 생성/수정 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

