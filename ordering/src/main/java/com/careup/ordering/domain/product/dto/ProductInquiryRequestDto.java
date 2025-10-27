package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.InquiryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInquiryRequestDto {
    
    private Long memberId;
    private Long branchProductId;
    private String title;
    private String content;
    private InquiryType inquiryType;
    private Boolean isSecret;
}
