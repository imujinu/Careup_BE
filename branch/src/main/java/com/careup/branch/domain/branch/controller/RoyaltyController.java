package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyDetailResponseDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyListResponseDto;
import com.careup.branch.domain.branch.dto.royalty.SettlementHistoryResponseDto;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import com.careup.branch.domain.branch.service.RoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> getAllRoyalties() {
        try {
            List<RoyaltyListResponseDto> royalties = royaltyService.getAllRoyalties();
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(royalties)
                            .status_code(HttpStatus.OK.value())
                            .status_message("로열티 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("로열티 목록 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점에 대한 로열티 상세 조회
     * GET /royalties/{royaltyId}
     */
    @GetMapping("/{royaltyId}")
    public ResponseEntity<?> getRoyaltyDetail(@PathVariable Long royaltyId) {
        try {
            RoyaltyDetailResponseDto royalty = royaltyService.getRoyaltyDetail(royaltyId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(royalty)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 로열티 상세 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 로열티 상세 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (전체)
     * GET /royalties/branches/{branchId}/settlements
     */
    @GetMapping("/branches/{branchId}/settlements")
    public ResponseEntity<?> getSettlementHistory(
            @PathVariable Long branchId) {
        try {
            List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistory(branchId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(history)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 정산 내역 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 정산 내역 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (정산 상태별 필터링)
     * GET /royalties/branches/{branchId}/settlements?status={status}
     */
    @GetMapping("/branches/{branchId}/settlements/filter")
    public ResponseEntity<?> getSettlementHistoryByStatus(
            @PathVariable Long branchId,
            @RequestParam SettlementStatus status) {
        try {
            List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistoryByStatus(branchId, status);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(history)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 정산 내역 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 정산 내역 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }
}
