package com.careup.ordering.domain.member.dto.response;

import com.careup.ordering.domain.member.entity.LoyalCustomer;
import com.careup.ordering.domain.member.entity.LoyalGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyalCustomerResponseDto {
    
    private Long id;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private Long branchId;
    private BigDecimal totalAmount;
    private Integer orderCount;
    private LoyalGrade grade;
    private LocalDateTime registeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static LoyalCustomerResponseDto from(LoyalCustomer loyalCustomer) {
        return LoyalCustomerResponseDto.builder()
                .id(loyalCustomer.getId())
                .memberId(loyalCustomer.getMemberId())
                .branchId(loyalCustomer.getBranchId())
                .totalAmount(loyalCustomer.getTotalAmount())
                .orderCount(loyalCustomer.getOrderCount())
                .grade(loyalCustomer.getGrade())
                .registeredAt(loyalCustomer.getRegisteredAt())
                .createdAt(loyalCustomer.getCreatedAt())
                .updatedAt(loyalCustomer.getUpdatedAt())
                .build();
    }
}
