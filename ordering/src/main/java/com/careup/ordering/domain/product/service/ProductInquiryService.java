package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.product.dto.ProductInquiryRequestDto;
import com.careup.ordering.domain.product.dto.ProductInquiryResponseDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InquiryStatus;
import com.careup.ordering.domain.product.entity.ProductInquiry;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.ProductInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductInquiryService {
    
    private final ProductInquiryRepository productInquiryRepository;
    private final BranchProductRepository branchProductRepository;
    private final MemberRepository memberRepository;
    
    /**
     *  상품 문의사항 작성
     */
    @Transactional
    public ProductInquiryResponseDto createInquiry(ProductInquiryRequestDto request) {
        // 회원 조회
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        
        // 지점 상품 조회
        BranchProduct branchProduct = branchProductRepository.findById(request.getBranchProductId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        
        // 문의 생성
        ProductInquiry inquiry = ProductInquiry.builder()
                .member(member)
                .branchProduct(branchProduct)
                .title(request.getTitle())
                .content(request.getContent())
                .inquiryType(request.getInquiryType())
                .isSecret(request.getIsSecret() != null ? request.getIsSecret() : false)
                .status(InquiryStatus.PENDING) // status 명시적 설정 추가 했음.
                .build();
        
        ProductInquiry saved = productInquiryRepository.save(inquiry);
        return ProductInquiryResponseDto.from(saved);
    }
    
    /**
     * 상품 문의 목록 조회 (지점 상품별)
     */
    public List<ProductInquiryResponseDto> getInquiriesByBranchProduct(Long branchProductId) {
        List<ProductInquiry> inquiries = productInquiryRepository.findByBranchProductId(branchProductId);
        return inquiries.stream()
                .map(ProductInquiryResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 내 문의 목록 조회
     */
    public List<ProductInquiryResponseDto> getMyInquiries(Long memberId) {
        List<ProductInquiry> inquiries = productInquiryRepository.findByMemberId(memberId);
        return inquiries.stream()
                .map(ProductInquiryResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 문의 상세 조회
     */
    public ProductInquiryResponseDto getInquiry(Long inquiryId) {
        ProductInquiry inquiry = productInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        return ProductInquiryResponseDto.from(inquiry);
    }
    
    /**
     * 문의 수정
     */
    @Transactional
    public ProductInquiryResponseDto updateInquiry(Long inquiryId, ProductInquiryRequestDto request) {
        ProductInquiry inquiry = productInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        
        // 엔티티의 업데이트 메서드 사용 (답변 여부 체크 포함)
        inquiry.updateInquiry(
                request.getTitle(),
                request.getContent(),
                request.getInquiryType(),
                request.getIsSecret()
        );
        
        return ProductInquiryResponseDto.from(inquiry);
    }
    
    /**
     * 문의 삭제
     */
    @Transactional
    public void deleteInquiry(Long inquiryId) {
        ProductInquiry inquiry = productInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
        
        // 답변이 달린 문의는 삭제 불가
        if (inquiry.getStatus() == InquiryStatus.ANSWERED) {
            throw new IllegalStateException("답변이 달린 문의는 삭제할 수 없습니다.");
        }
        
        productInquiryRepository.delete(inquiry);
    }
    
    /**
     * 답변 대기 중인 문의 목록 조회
     */
    public List<ProductInquiryResponseDto> getPendingInquiries() {
        List<ProductInquiry> inquiries = productInquiryRepository.findByStatus(InquiryStatus.PENDING);
        return inquiries.stream()
                .map(ProductInquiryResponseDto::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 상품별 답변 대기 중인 문의 조회
     */
    public List<ProductInquiryResponseDto> getPendingInquiriesByProduct(Long branchProductId) {
        List<ProductInquiry> inquiries = productInquiryRepository
                .findByBranchProductIdAndStatus(branchProductId, InquiryStatus.PENDING);
        return inquiries.stream()
                .map(ProductInquiryResponseDto::from)
                .collect(Collectors.toList());
    }
}
