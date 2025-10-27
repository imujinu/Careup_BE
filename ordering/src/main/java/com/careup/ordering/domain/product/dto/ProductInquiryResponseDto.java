package com.careup.ordering.domain.product.dto;

import com.careup.ordering.domain.product.entity.InquiryStatus;
import com.careup.ordering.domain.product.entity.InquiryType;
import com.careup.ordering.domain.product.entity.ProductInquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInquiryResponseDto {
    
    private Long id;
    private Long memberId;
    private String memberName;
    private Long branchProductId;
    private String productName;
    private String title;
    private String content;
    private InquiryType inquiryType;
    private InquiryStatus status;
    private Boolean isSecret;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer answerCount;
    
    public static ProductInquiryResponseDto from(ProductInquiry inquiry) {
        return ProductInquiryResponseDto.builder()
                .id(inquiry.getId())
                .memberId(inquiry.getMember().getId())
                .memberName(inquiry.getMember().getName())
                .branchProductId(inquiry.getBranchProduct().getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .inquiryType(inquiry.getInquiryType())
                .status(inquiry.getStatus())
                .isSecret(inquiry.getIsSecret())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .answerCount(inquiry.getAnswers() != null ? inquiry.getAnswers().size() : 0)
                .build();
    }
}
