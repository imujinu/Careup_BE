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
    public SalesSummaryDto getSalesSummary(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("[Ordering API] 매출 현황 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
        SalesSummaryDto response = dashboardService.getSalesSummary(branchId, startDate, endDate);
        log.info("[Ordering API] 매출 현황 조회 응답 - totalSales: {}, monthlySales: {}, totalOrders: {}",
                response.getTotalSales(), response.getMonthlySales(), response.getTotalOrders());
        return response;
    }

    /**
     * 재고 현황 조회 (총 재고 품목, 재고 부족 품목, 재고 충족률, 재고 알림)
     * GET /api/dashboard/inventory-summary?branchId=1
     */
    @GetMapping("/inventory-summary")
    public InventorySummaryDto getInventorySummary(@RequestParam Long branchId) {

        log.info("[Ordering API] 재고 현황 조회 요청 - branchId: {}", branchId);
        InventorySummaryDto response = dashboardService.getInventorySummary(branchId);
        log.info("[Ordering API] 재고 현황 조회 응답 - totalProducts: {}, lowStockProducts: {}",
                response.getTotalProducts(), response.getLowStockProducts());
        return response;
    }

    /**
     * 주문 현황 조회 (총 주문 수, 처리 완료, 대기 중, 취소)
     * GET /api/dashboard/order-summary?branchId=1&startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/order-summary")
    public OrderSummaryDto getOrderSummary(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("[Ordering API] 주문 현황 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
        OrderSummaryDto response = dashboardService.getOrderSummary(branchId, startDate, endDate);
        log.info("[Ordering API] 주문 현황 조회 응답 - totalOrders: {}, completedOrders: {}",
                response.getTotalOrders(), response.getCompletedOrders());
        return response;
    }

    /**
     * 기간별 매출 추이 조회 (연간, 월간, 주간)
     * GET /api/dashboard/sales-trend?branchId=1&period=MONTHLY&year=2025
     */
    @GetMapping("/sales-trend")
    public SalesTrendDto getSalesTrend(
            @RequestParam Long branchId,
            @RequestParam String period, // "YEARLY", "MONTHLY", "WEEKLY"
            @RequestParam(required = false) Integer year) {

        log.info("[Ordering API] 매출 추이 조회 요청 - branchId: {}, period: {}, year: {}", branchId, period, year);
        SalesTrendDto response = dashboardService.getSalesTrend(branchId, period, year);
        log.info("[Ordering API] 매출 추이 조회 응답 - period: {}, totalSales: {}",
                response.getPeriod(), response.getTotalSales());
        return response;
    }

    /**
     * 카테고리별 매출 분석
     * GET /api/dashboard/category-sales?branchId=1&startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/category-sales")
    public CategorySalesDto getCategorySales(
            @RequestParam Long branchId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("[Ordering API] 카테고리별 매출 조회 요청 - branchId: {}, startDate: {}, endDate: {}", branchId, startDate, endDate);
        CategorySalesDto response = dashboardService.getCategorySales(branchId, startDate, endDate);
        log.info("[Ordering API] 카테고리별 매출 조회 응답 - totalSales: {}, topCategory: {}",
                response.getTotalSales(), response.getTopCategory());
        return response;
    }
}

