package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import com.careup.ordering.domain.order.dto.response.TopBranchResponseDto;
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
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
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
     * 이달의 우수 지점 (월 기준 총 매출 1위)
     * GET /hq/sales/top-branch?year=2025&month=11
     * year, month가 없으면 현재 월 기준
     */
    @GetMapping("/top-branch")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<?> getTopBranch(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            TopBranchResponseDto response = hqSalesService.getTopBranchOfMonth(year, month);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("이달의 우수 지점 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("이달의 우수 지점 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 전체 지점 상품 매출 TOP/BOTTOM (기간 내)
     * GET /hq/sales/products?startDate=2025-01-01&endDate=2025-01-31&sortType=HIGH_SALES&size=1
     * sortType: HIGH_SALES | LOW_SALES | HIGH_MARGIN | LOW_MARGIN
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<?> getHqProductSales(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "HIGH_SALES") String sortType,
            @RequestParam(defaultValue = "10") Integer size) {

        try {
            var products = hqSalesService.getHqProductSales(startDate, endDate, sortType, size);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(products)
                            .status_code(HttpStatus.OK.value())
                            .status_message("전체 지점 상품 매출 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("전체 지점 상품 매출 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 매출 내역 기간별 조회
     * GET /hq/sales/branch/{branchId}?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
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
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
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

