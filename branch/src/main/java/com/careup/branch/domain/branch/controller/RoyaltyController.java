package com.careup.branch.domain.branch.controller;

import com.careup.branch.domain.branch.dto.royalty.RoyaltyDetailResponseDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyListResponseDto;
import com.careup.branch.domain.branch.dto.royalty.SettlementHistoryResponseDto;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import com.careup.branch.domain.branch.service.RoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/royalties")
@RequiredArgsConstructor
public class RoyaltyController {

    private final RoyaltyService royaltyService;

    /**
     * 지점 전체 로열티 조회
     * GET /royalties
     */
    @GetMapping
    public ResponseEntity<List<RoyaltyListResponseDto>> getAllRoyalties() {
        List<RoyaltyListResponseDto> royalties = royaltyService.getAllRoyalties();
        return ResponseEntity.ok(royalties);
    }

    /**
     * 선택한 가맹점에 대한 로열티 상세 조회
     * GET /royalties/{royaltyId}
     */
    @GetMapping("/{royaltyId}")
    public ResponseEntity<RoyaltyDetailResponseDto> getRoyaltyDetail(@PathVariable Long royaltyId) {
        RoyaltyDetailResponseDto royalty = royaltyService.getRoyaltyDetail(royaltyId);
        return ResponseEntity.ok(royalty);
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (전체)
     * GET /royalties/branches/{branchId}/settlements
     */
    @GetMapping("/branches/{branchId}/settlements")
    public ResponseEntity<List<SettlementHistoryResponseDto>> getSettlementHistory(
            @PathVariable Long branchId) {
        List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistory(branchId);
        return ResponseEntity.ok(history);
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (정산 상태별 필터링)
     * GET /royalties/branches/{branchId}/settlements?status={status}
     */
    @GetMapping("/branches/{branchId}/settlements/filter")
    public ResponseEntity<List<SettlementHistoryResponseDto>> getSettlementHistoryByStatus(
            @PathVariable Long branchId,
            @RequestParam SettlementStatus status) {
        List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistoryByStatus(branchId, status);
        return ResponseEntity.ok(history);
    }
}
