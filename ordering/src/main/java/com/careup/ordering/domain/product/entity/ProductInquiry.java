package com.careup.ordering.domain.product.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import com.careup.ordering.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_inquiry")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_products_id", nullable = false)
    private BranchProduct branchProduct;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false)
    private InquiryType inquiryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "is_secret", nullable = false)
    private Boolean isSecret;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductInquiryAnswer> answers = new ArrayList<>();
    
    // 비즈니스 로직 메서드
    public void updateInquiry(String title, String content, InquiryType inquiryType, Boolean isSecret) {
        if (this.status == InquiryStatus.ANSWERED) {
            throw new IllegalStateException("답변이 달린 문의는 수정할 수 없습니다.");
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (inquiryType != null) {
            this.inquiryType = inquiryType;
        }
        if (isSecret != null) {
            this.isSecret = isSecret;
        }
    }
    
    public void answer() {
        this.status = InquiryStatus.ANSWERED;
    }
    
    public void close() {
        this.status = InquiryStatus.CLOSED;
    }
}
