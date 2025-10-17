package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import com.careup.ordering.domain.order.service.HqSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 본점 관리자(HQ_ADMIN) 전용 매출 관련 API
 */
@RestController
@RequestMapping("/hq/sales")
@RequiredArgsConstructor
public class HqSalesController {

    private final HqSalesService hqSalesService;

    /**
     * 전체 지점 매출 내역 기간별 조회
     * GET /hq/sales/all?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> getAllBranchesSales(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            HqSalesRequestDto request = HqSalesRequestDto.withDates(startDate, endDate, periodType);
            AllBranchesSalesResponseDto response = hqSalesService.getAllBranchesSales(request);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("전체 지점 매출 내역 기간별 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("전체 지점 매출 내역 기간별 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 매출 내역 기간별 조회
     * GET /hq/sales/branch/{branchId}?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> getBranchSalesDetail(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            HqSalesRequestDto request = HqSalesRequestDto.withDates(startDate, endDate, periodType);
            BranchSalesDetailResponseDto response = hqSalesService.getBranchSalesDetail(branchId, request);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점의 매출 내역 기간별 조회")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("선택한 가맹점 매출 내역 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 가맹점 간 매출 비교
     * GET /hq/sales/comparison?branchIds=1,2,3&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/comparison")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> compareBranchesSales(
            @RequestParam List<Long> branchIds,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            HqSalesRequestDto request = HqSalesRequestDto.withDatesAndBranchIds(branchIds, startDate, endDate, periodType);
            BranchComparisonResponseDto response = hqSalesService.compareBranchesSales(request);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("가맹점 간 매출 비교 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("가맹점 간 매출 비교 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }
}

