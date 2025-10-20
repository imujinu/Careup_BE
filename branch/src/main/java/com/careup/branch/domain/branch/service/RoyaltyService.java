package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.client.OrderClient;
import com.careup.branch.domain.branch.dto.royalty.OrderSalesResponseDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyDetailResponseDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyListResponseDto;
import com.careup.branch.domain.branch.dto.royalty.SettlementHistoryResponseDto;
import com.careup.branch.domain.branch.entity.Royalty;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import com.careup.branch.domain.branch.repository.RoyaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoyaltyService {

    private final RoyaltyRepository royaltyRepository;
    private final OrderClient orderClient;

    /**
     * 지점 전체 로열티 조회
     */
    public List<RoyaltyListResponseDto> getAllRoyalties() {
        log.info("전체 로열티 목록 조회");

        List<Royalty> royalties = royaltyRepository.findAllWithBranch();

        return royalties.stream()
                .map(this::convertToListDto)
                .collect(Collectors.toList());
    }

    /**
     * 선택한 가맹점에 대한 로열티 상세 조회
     */
    public RoyaltyDetailResponseDto getRoyaltyDetail(Long royaltyId) {
        log.info("로열티 상세 조회 - royaltyId: {}", royaltyId);

        Royalty royalty = royaltyRepository.findByIdWithBranch(royaltyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 로열티 정보를 찾을 수 없습니다. ID: " + royaltyId));

        // Ordering 모듈에서 매출 정보 조회
        Long totalSales = 0L;
        try {
            OrderSalesResponseDto salesDto = orderClient.getBranchSales(
                    royalty.getBranch().getId(),
                    royalty.getApplicableMonth()
            );
            totalSales = salesDto.getTotalSales();
        } catch (Exception e) {
            log.warn("매출 정보 조회 실패 - branchId: {}, month: {}",
                    royalty.getBranch().getId(), royalty.getApplicableMonth(), e);
        }

        return convertToDetailDto(royalty, totalSales);
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (전체)
     */
    public List<SettlementHistoryResponseDto> getSettlementHistory(Long branchId) {
        log.info("정산 내역 조회 - branchId: {}", branchId);

        List<Royalty> royalties = royaltyRepository.findByBranchIdOrderByApplicableMonthDesc(branchId);

        return royalties.stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (정산 상태별 필터링)
     */
    public List<SettlementHistoryResponseDto> getSettlementHistoryByStatus(Long branchId, SettlementStatus status) {
        log.info("정산 내역 조회 (상태별) - branchId: {}, status: {}", branchId, status);

        List<Royalty> royalties = royaltyRepository.findByBranchIdAndSettlementStatus(branchId, status);

        return royalties.stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    // DTO 변환 메서드
    private RoyaltyListResponseDto convertToListDto(Royalty royalty) {
        return RoyaltyListResponseDto.builder()
                .royaltyId(royalty.getId())
                .branchId(royalty.getBranch().getId())
                .branchName(royalty.getBranch().getName())
                .applicableMonth(royalty.getApplicableMonth())
                .calculationMethod(royalty.getCalculationMethod())
                .percentage(royalty.getPercentage())
                .fixedAmount(royalty.getFixedAmount())
                .amount(royalty.getAmount())
                .dueDate(royalty.getDueDate())
                .settlementStatus(royalty.getSettlementStatus())
                .build();
    }

    private RoyaltyDetailResponseDto convertToDetailDto(Royalty royalty, Long totalSales) {
        return RoyaltyDetailResponseDto.builder()
                .royaltyId(royalty.getId())
                .applicableMonth(royalty.getApplicableMonth())
                .calculationMethod(royalty.getCalculationMethod())
                .percentage(royalty.getPercentage())
                .fixedAmount(royalty.getFixedAmount())
                .amount(royalty.getAmount())
                .dueDate(royalty.getDueDate())
                .settlementStatus(royalty.getSettlementStatus())
                .paymentDate(royalty.getPaymentDate())
                .branchId(royalty.getBranch().getId())
                .branchName(royalty.getBranch().getName())
                .businessNumber(royalty.getBranch().getBusinessNumber())
                .phone(royalty.getBranch().getPhone())
                .email(royalty.getBranch().getEmail())
                .address(royalty.getBranch().getAddress())
                .ownershipType(royalty.getBranch().getOwnershipType())
                .totalSales(totalSales)
                .build();
    }

    private SettlementHistoryResponseDto convertToHistoryDto(Royalty royalty) {
        return SettlementHistoryResponseDto.builder()
                .royaltyId(royalty.getId())
                .applicableMonth(royalty.getApplicableMonth())
                .calculationMethod(royalty.getCalculationMethod())
                .percentage(royalty.getPercentage())
                .fixedAmount(royalty.getFixedAmount())
                .amount(royalty.getAmount())
                .dueDate(royalty.getDueDate())
                .settlementStatus(royalty.getSettlementStatus())
                .paymentDate(royalty.getPaymentDate())
                .createdAt(royalty.getCreatedAt())
                .build();
    }
}
