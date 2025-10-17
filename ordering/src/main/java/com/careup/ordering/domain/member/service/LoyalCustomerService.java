package com.careup.ordering.domain.member.service;

import com.careup.ordering.domain.member.dto.request.LoyalCustomerRegisterRequest;
import com.careup.ordering.domain.member.dto.request.LoyalCustomerUpdateRequest;
import com.careup.ordering.domain.member.dto.response.LoyalCustomerResponseDto;
import com.careup.ordering.domain.member.entity.LoyalCustomer;
import com.careup.ordering.domain.member.entity.LoyalGrade;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.LoyalCustomerRepository;
import com.careup.ordering.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyalCustomerService {

    private final LoyalCustomerRepository loyalCustomerRepository;
    private final MemberRepository memberRepository;

    /**
     * REQ-074: 단골 고객 조회 (지점별)
     */
    public List<LoyalCustomerResponseDto> getLoyalCustomersByBranch(Long branchId) {
        List<LoyalCustomer> loyalCustomers = loyalCustomerRepository.findByBranchId(branchId);
        return loyalCustomers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * REQ-074: 단골 고객 상세 조회
     */
    public LoyalCustomerResponseDto getLoyalCustomer(Long loyalCustomerId) {
        LoyalCustomer loyalCustomer = loyalCustomerRepository.findById(loyalCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단골 고객을 찾을 수 없습니다."));
        return convertToDto(loyalCustomer);
    }

    /**
     * REQ-075: 단골 고객 등록
     * 수동 등록 또는 자동 등록 (주문 금액 기준)
     */
    @Transactional
    public LoyalCustomerResponseDto registerLoyalCustomer(LoyalCustomerRegisterRequest request) {
        // 이미 단골 고객인지 확인
        if (loyalCustomerRepository.existsByMemberIdAndBranchId(
                request.getMemberId(), request.getBranchId())) {
            throw new IllegalArgumentException("이미 단골 고객으로 등록되어 있습니다.");
        }

        // 회원 존재 확인
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 단골 고객 생성
        LoyalCustomer loyalCustomer = LoyalCustomer.builder()
                .memberId(request.getMemberId())
                .branchId(request.getBranchId())
                .totalAmount(request.getInitialAmount() != null ?
                        request.getInitialAmount() : BigDecimal.ZERO)
                .orderCount(request.getInitialOrderCount() != null ?
                        request.getInitialOrderCount() : 0)
                .grade(LoyalGrade.BRONZE)
                .registeredAt(LocalDateTime.now())
                .build();

        LoyalCustomer saved = loyalCustomerRepository.save(loyalCustomer);
        return convertToDto(saved);
    }

    /**
     * REQ-076: 단골 고객 수정
     */
    @Transactional
    public LoyalCustomerResponseDto updateLoyalCustomer(Long loyalCustomerId,
                                                        LoyalCustomerUpdateRequest request) {
        LoyalCustomer loyalCustomer = loyalCustomerRepository.findById(loyalCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단골 고객을 찾을 수 없습니다."));

        // 엔티티의 업데이트 메서드 사용
        loyalCustomer.updateInfo(
                request.getTotalAmount(),
                request.getOrderCount(),
                request.getGrade()
        );

        return convertToDto(loyalCustomer);
    }

    /**
     * REQ-076: 단골 고객 삭제
     */
    @Transactional
    public void deleteLoyalCustomer(Long loyalCustomerId) {
        LoyalCustomer loyalCustomer = loyalCustomerRepository.findById(loyalCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단골 고객을 찾을 수 없습니다."));
        loyalCustomerRepository.delete(loyalCustomer);
    }

    /**
     * 주문 완료 시 단골 고객 정보 업데이트 (자동)
     * 일정 금액 이상 구매 시 자동으로 단골 고객으로 등록
     */
    @Transactional
    public void updateLoyalCustomerByOrder(Long memberId, Long branchId, BigDecimal orderAmount) {
        loyalCustomerRepository.findByMemberIdAndBranchId(memberId, branchId)
                .ifPresentOrElse(
                        loyalCustomer -> {
                            // 기존 단골 고객이면 금액 업데이트
                            loyalCustomer.updateAmount(orderAmount);
                        },
                        () -> {
                            // 신규 고객이고 일정 금액(예: 10만원) 이상 구매 시 자동 등록
                            if (orderAmount.compareTo(new BigDecimal("100000")) >= 0) {
                                LoyalCustomer newLoyalCustomer = LoyalCustomer.builder()
                                        .memberId(memberId)
                                        .branchId(branchId)
                                        .totalAmount(orderAmount)
                                        .orderCount(1)
                                        .grade(LoyalGrade.BRONZE)
                                        .registeredAt(LocalDateTime.now())
                                        .build();
                                loyalCustomerRepository.save(newLoyalCustomer);
                            }
                        }
                );
    }

    /**
     * 등급별 단골 고객 조회
     */
    public List<LoyalCustomerResponseDto> getLoyalCustomersByGrade(Long branchId,
                                                                    LoyalGrade grade) {
        List<LoyalCustomer> loyalCustomers = loyalCustomerRepository.findByBranchIdAndGrade(branchId, grade);
        return loyalCustomers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * LoyalCustomer 엔티티를 DTO로 변환 (회원 정보 포함)
     */
    private LoyalCustomerResponseDto convertToDto(LoyalCustomer loyalCustomer) {
        LoyalCustomerResponseDto.LoyalCustomerResponseDtoBuilder builder =
                LoyalCustomerResponseDto.builder()
                        .id(loyalCustomer.getId())
                        .memberId(loyalCustomer.getMemberId())
                        .branchId(loyalCustomer.getBranchId())
                        .totalAmount(loyalCustomer.getTotalAmount())
                        .orderCount(loyalCustomer.getOrderCount())
                        .grade(loyalCustomer.getGrade())
                        .registeredAt(loyalCustomer.getRegisteredAt())
                        .createdAt(loyalCustomer.getCreatedAt())
                        .updatedAt(loyalCustomer.getUpdatedAt());

        // 회원 정보 추가
        memberRepository.findById(loyalCustomer.getMemberId())
                .ifPresent(member -> {
                    builder.memberName(member.getName());
                    builder.memberEmail(member.getEmail());
                });

        return builder.build();
    }
}
