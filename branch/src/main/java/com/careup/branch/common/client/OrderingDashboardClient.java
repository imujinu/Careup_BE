package com.careup.branch.common.client;

import com.careup.branch.common.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Ordering 서비스의 대시보드 관련 API를 호출하는 FeignClient
 * Eureka를 통해 ordering-service와 직접 통신
 */
@FeignClient(
        name = "ordering-service",
        configuration = FeignConfig.class
)
public interface OrderingDashboardClient {

    /**
     * 매출 현황 조회 (총 매출, 월간 매출, 총 주문 수, 최근 7일 매출)
     */
    @GetMapping("/api/dashboard/sales-summary")
    SalesSummaryDto getSalesSummary(
            @RequestParam("branchId") Long branchId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    );

    /**
     * 재고 현황 조회 (총 재고 품목, 재고 부족 품목, 재고 충족률, 재고 알림)
     */
    @GetMapping("/api/dashboard/inventory-summary")
    InventorySummaryDto getInventorySummary(@RequestParam("branchId") Long branchId);

    /**
     * 주문 현황 조회 (총 주문 수, 처리 완료, 대기 중, 취소)
     */
    @GetMapping("/api/dashboard/order-summary")
    OrderSummaryDto getOrderSummary(
            @RequestParam("branchId") Long branchId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    );

    /**
     * 기간별 매출 추이 조회 (연간, 월간, 주간)
     */
    @GetMapping("/api/dashboard/sales-trend")
    SalesTrendDto getSalesTrend(
            @RequestParam("branchId") Long branchId,
            @RequestParam("period") String period, // "YEARLY", "MONTHLY", "WEEKLY"
            @RequestParam(value = "year", required = false) Integer year
    );

    /**
     * 카테고리별 매출 분석
     */
    @GetMapping("/api/dashboard/category-sales")
    CategorySalesDto getCategorySales(
            @RequestParam("branchId") Long branchId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    );

    // ====== DTOs ======

    /**
     * 매출 현황
     */
    class SalesSummaryDto {
        public Long totalSales;           // 총 매출
        public Long monthlySales;         // 월간 매출
        public Long totalOrders;          // 총 주문 수
        public List<DailySalesDto> last7DaysSales;  // 최근 7일 매출 (차트용)
    }

    class DailySalesDto {
        public LocalDate date;
        public Long sales;
    }

    /**
     * 재고 현황
     */
    class InventorySummaryDto {
        public Long totalProducts;        // 총 재고 품목
        public Long lowStockProducts;     // 재고 부족 품목
        public Double stockFulfillmentRate;  // 재고 충족률 (%)
        public List<StockAlertDto> stockAlerts;  // 재고 알림
    }

    class StockAlertDto {
        public Long productId;
        public String productName;
        public Long currentStock;
        public Long safetyStock;
        public String alertLevel;  // "CRITICAL", "WARNING"
    }

    /**
     * 주문 현황
     */
    class OrderSummaryDto {
        public Long totalOrders;          // 총 주문 수
        public Long completedOrders;      // 처리 완료 건수
        public Long pendingOrders;        // 대기 중인 건수
        public Long canceledOrders;       // 취소된 건수
        public Map<String, Long> orderStatusDistribution;  // 주문 상태 분포 (차트용)
    }

    /**
     * 매출 추이
     */
    class SalesTrendDto {
        public String period;             // "YEARLY", "MONTHLY", "WEEKLY"
        public List<PeriodSalesDto> salesData;  // 기간별 매출 데이터 (차트용)
        public Long totalSales;           // 올해 총 매출
        public Double yearOverYearGrowth; // 전년 대비 증감률 (%)
        public Double goalAchievementRate; // 목표 달성률 (%)
    }

    class PeriodSalesDto {
        public String periodLabel;  // "2025-01", "Week 1" 등
        public Long sales;
    }

    /**
     * 카테고리별 매출 분석
     */
    class CategorySalesDto {
        public Map<String, Long> categorySalesDistribution;  // 카테고리별 매출 비중 (차트용)
        public Long totalSales;           // 총 매출
        public String topCategory;        // 최고 카테고리
        public Long topCategorySales;     // 최고 카테고리 매출
    }
}

