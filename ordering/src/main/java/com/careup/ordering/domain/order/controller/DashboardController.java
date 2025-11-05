package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.order.dto.dashboard.*;
import com.careup.ordering.domain.order.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 대시보드 API 컨트롤러
 * Branch 서비스의 FeignClient가 호출하는 대시보드 관련 API
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 매출 현황 조회 (총 매출, 월간 매출, 총 주문 수, 최근 7일 매출)
     * GET /api/dashboard/sales-summary?branchId=1&startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/sales-summary")
    public ResponseEntity<?> getSalesSummary(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        try {
            log.info("매출 현황 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
            SalesSummaryDto response = dashboardService.getSalesSummary(branchId, startDate, endDate);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("매출 현황 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            log.error("매출 현황 조회 실패 - branchId: {}, error: {}", branchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("매출 현황 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 재고 현황 조회 (총 재고 품목, 재고 부족 품목, 재고 충족률, 재고 알림)
     * GET /api/dashboard/inventory-summary?branchId=1
     */
    @GetMapping("/inventory-summary")
    public ResponseEntity<?> getInventorySummary(@RequestParam Long branchId) {

        try {
            log.info("재고 현황 조회 요청 - branchId: {}", branchId);
            InventorySummaryDto response = dashboardService.getInventorySummary(branchId);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("재고 현황 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            log.error("재고 현황 조회 실패 - branchId: {}, error: {}", branchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("재고 현황 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 주문 현황 조회 (총 주문 수, 처리 완료, 대기 중, 취소)
     * GET /api/dashboard/order-summary?branchId=1&startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/order-summary")
    public ResponseEntity<?> getOrderSummary(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        try {
            log.info("주문 현황 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
            OrderSummaryDto response = dashboardService.getOrderSummary(branchId, startDate, endDate);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("주문 현황 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            log.error("주문 현황 조회 실패 - branchId: {}, error: {}", branchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("주문 현황 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 기간별 매출 추이 조회 (연간, 월간, 주간)
     * GET /api/dashboard/sales-trend?branchId=1&period=MONTHLY&year=2025
     */
    @GetMapping("/sales-trend")
    public ResponseEntity<?> getSalesTrend(
            @RequestParam Long branchId,
            @RequestParam String period, // "YEARLY", "MONTHLY", "WEEKLY"
            @RequestParam(required = false) Integer year) {

        try {
            log.info("매출 추이 조회 요청 - branchId: {}, period: {}, year: {}", branchId, period, year);
            SalesTrendDto response = dashboardService.getSalesTrend(branchId, period, year);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("매출 추이 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            log.error("매출 추이 조회 실패 - branchId: {}, period: {}, error: {}", branchId, period, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("매출 추이 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 카테고리별 매출 분석
     * GET /api/dashboard/category-sales?branchId=1&startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/category-sales")
    public ResponseEntity<?> getCategorySales(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        try {
            log.info("카테고리별 매출 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
            CategorySalesDto response = dashboardService.getCategorySales(branchId, startDate, endDate);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("카테고리별 매출 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            log.error("카테고리별 매출 조회 실패 - branchId: {}, error: {}", branchId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("카테고리별 매출 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }
}

