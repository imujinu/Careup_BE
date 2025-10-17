package com.careup.branch.domain.branch.dto.royalty;

import com.careup.branch.domain.branch.entity.CalculationMethod;
import com.careup.branch.domain.branch.entity.OwnershipType;
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
public class RoyaltyDetailResponseDto {
    // 로열티 정보
    private Long royaltyId;
    private String applicableMonth;
    private CalculationMethod calculationMethod;
    private BigDecimal percentage;
    private Long fixedAmount;
    private Long amount;
    private LocalDate dueDate;
    private SettlementStatus settlementStatus;
    private LocalDateTime paymentDate;

    // 지점 정보
    private Long branchId;
    private String branchName;
    private String businessNumber;
    private String phone;
    private String email;
    private String address;
    private OwnershipType ownershipType;

    // 매출 정보 (ordering 모듈에서 조회)
    private Long totalSales;
}

