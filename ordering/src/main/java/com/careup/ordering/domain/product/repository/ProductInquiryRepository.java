package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.InquiryStatus;
import com.careup.ordering.domain.product.entity.ProductInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductInquiryRepository extends JpaRepository<ProductInquiry, Long> {
    
    /**
     * 특정 지점 상품의 문의 목록 조회
     */
    List<ProductInquiry> findByBranchProductId(Long branchProductId);
    
    /**
     * 특정 회원의 문의 목록 조회
     */
    List<ProductInquiry> findByMemberId(Long memberId);
    
    /**
     * 상태별 문의 조회
     */
    List<ProductInquiry> findByStatus(InquiryStatus status);
    
    /**
     * 특정 지점 상품의 특정 상태 문의 조회
     */
    List<ProductInquiry> findByBranchProductIdAndStatus(Long branchProductId, InquiryStatus status);
}
