package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.order.dto.dashboard.*;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 대시보드 서비스
 * Branch 서비스의 FeignClient 요청을 처리하는 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderedItemRepository orderedItemRepository;
    private final BranchProductRepository branchProductRepository;

    /**
     * 매출 현황 조회 (총 매출, 월간 매출, 총 주문 수, 최근 7일 매출)
     */
    public SalesSummaryDto getSalesSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("매출 현황 조회 시작 - branchId: {}", branchId);

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 총 매출 계산
        Long totalSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 월간 매출 (현재 월)
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();
        Long monthlySales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED,
                monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));

        // 총 주문 수
        Long totalOrders = orderRepository.countOrdersByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 최근 7일 매출
        List<DailySalesDto> last7DaysSales = getLast7DaysSales(branchId);

        return SalesSummaryDto.builder()
                .totalSales(totalSales != null ? totalSales : 0L)
                .monthlySales(monthlySales != null ? monthlySales : 0L)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .last7DaysSales(last7DaysSales)
                .build();
    }

    /**
     * 최근 7일 매출 계산
     */
    private List<DailySalesDto> getLast7DaysSales(Long branchId) {
        List<DailySalesDto> result = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        List<Order> orders = orderRepository.findByBranchIdAndOrderStatusAndCreatedAtBetween(
                branchId, OrderStatus.CONFIRMED,
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        // 일별 매출 그룹화
        Map<LocalDate, Long> dailySalesMap = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.summingLong(Order::getTotalAmount)
                ));

        // 7일간 데이터 채우기 (데이터 없는 날은 0으로)
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            Long sales = dailySalesMap.getOrDefault(date, 0L);
            result.add(DailySalesDto.builder()
                    .date(date)
                    .sales(sales)
                    .build());
        }

        return result;
    }

    /**
     * 재고 현황 조회 (총 재고 품목, 재고 부족 품목, 재고 충족률, 재고 알림)
     */
    public InventorySummaryDto getInventorySummary(Long branchId) {
        log.info("재고 현황 조회 시작 - branchId: {}", branchId);

        List<BranchProduct> branchProducts = branchProductRepository.findByBranchId(branchId);

        // 총 재고 품목
        Long totalProducts = (long) branchProducts.size();

        // 재고 부족 품목 (안전재고 이하)
        Long lowStockProducts = branchProducts.stream()
                .filter(bp -> bp.getStockQuantity() <= bp.getSafetystock())
                .count();

        // 재고 충족률 계산
        Double stockFulfillmentRate = totalProducts > 0
                ? ((totalProducts - lowStockProducts) * 100.0 / totalProducts)
                : 100.0;

        // 재고 알림 (재고 부족 상위 10개)
        List<StockAlertDto> stockAlerts = branchProducts.stream()
                .filter(bp -> bp.getStockQuantity() <= bp.getSafetystock())
                .sorted(Comparator.comparing(bp -> bp.getStockQuantity() - bp.getSafetystock()))
                .limit(10)
                .map(bp -> {
                    String alertLevel = bp.getStockQuantity() == 0 ? "CRITICAL" : "WARNING";
                    return StockAlertDto.builder()
                            .productId(bp.getProduct().getId())
                            .productName(bp.getProduct().getName())
                            .currentStock(bp.getStockQuantity())
                            .safetyStock(bp.getSafetystock())
                            .alertLevel(alertLevel)
                            .build();
                })
                .collect(Collectors.toList());

        return InventorySummaryDto.builder()
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .stockFulfillmentRate(Math.round(stockFulfillmentRate * 100.0) / 100.0)
                .stockAlerts(stockAlerts)
                .build();
    }

    /**
     * 주문 현황 조회 (총 주문 수, 처리 완료, 대기 중, 취소)
     */
    public OrderSummaryDto getOrderSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("주문 현황 조회 시작 - branchId: {}", branchId);

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 기간별 주문 조회
        List<Order> allOrders = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime)
                .stream()
                .filter(order -> order.getBranchId().equals(branchId))
                .collect(Collectors.toList());

        // 상태별 주문 수 계산
        Map<OrderStatus, Long> statusCount = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));

        Long totalOrders = (long) allOrders.size();
        Long completedOrders = statusCount.getOrDefault(OrderStatus.CONFIRMED, 0L);
        Long pendingOrders = statusCount.getOrDefault(OrderStatus.PENDING, 0L);
        Long canceledOrders = statusCount.getOrDefault(OrderStatus.CANCELLED, 0L);

        // 주문 상태 분포 (차트용)
        Map<String, Long> orderStatusDistribution = new HashMap<>();
        orderStatusDistribution.put("CONFIRMED", completedOrders);
        orderStatusDistribution.put("PENDING", pendingOrders);
        orderStatusDistribution.put("CANCELLED", canceledOrders);

        return OrderSummaryDto.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .canceledOrders(canceledOrders)
                .orderStatusDistribution(orderStatusDistribution)
                .build();
    }

    /**
     * 기간별 매출 추이 조회 (연간, 월간, 주간)
     */
    public SalesTrendDto getSalesTrend(Long branchId, String period, Integer year) {
        log.info("매출 추이 조회 시작 - branchId: {}, period: {}, year: {}", branchId, period, year);

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        List<PeriodSalesDto> salesData;
        Long totalSales = 0L;
        Double yearOverYearGrowth = 0.0;

        switch (period.toUpperCase()) {
            case "YEARLY":
                salesData = getYearlySalesTrend(branchId, year);
                break;
            case "MONTHLY":
                salesData = getMonthlySalesTrend(branchId, year);
                break;
            case "WEEKLY":
                salesData = getWeeklySalesTrend(branchId, year);
                break;
            default:
                salesData = getMonthlySalesTrend(branchId, year);
        }

        // 총 매출 계산
        totalSales = salesData.stream()
                .mapToLong(PeriodSalesDto::getSales)
                .sum();

        // 전년 대비 증감률 계산 (간단히 0으로 설정, 실제 구현 시 전년도 데이터 필요)
        yearOverYearGrowth = 0.0;

        // 목표 달성률 (간단히 100으로 설정, 실제 구현 시 목표 데이터 필요)
        Double goalAchievementRate = 100.0;

        return SalesTrendDto.builder()
                .period(period)
                .salesData(salesData)
                .totalSales(totalSales)
                .yearOverYearGrowth(yearOverYearGrowth)
                .goalAchievementRate(goalAchievementRate)
                .build();
    }

    /**
     * 연도별 매출 추이
     */
    private List<PeriodSalesDto> getYearlySalesTrend(Long branchId, Integer targetYear) {
        List<PeriodSalesDto> result = new ArrayList<>();

        // 최근 5년간 데이터
        for (int i = 4; i >= 0; i--) {
            int year = targetYear - i;
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);

            Long sales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                    branchId, OrderStatus.CONFIRMED,
                    startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

            result.add(PeriodSalesDto.builder()
                    .periodLabel(String.valueOf(year))
                    .sales(sales != null ? sales : 0L)
                    .build());
        }

        return result;
    }

    /**
     * 월별 매출 추이
     */
    private List<PeriodSalesDto> getMonthlySalesTrend(Long branchId, Integer year) {
        List<PeriodSalesDto> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            Long sales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                    branchId, OrderStatus.CONFIRMED,
                    startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

            result.add(PeriodSalesDto.builder()
                    .periodLabel(String.format("%d-%02d", year, month))
                    .sales(sales != null ? sales : 0L)
                    .build());
        }

        return result;
    }

    /**
     * 주별 매출 추이
     */
    private List<PeriodSalesDto> getWeeklySalesTrend(Long branchId, Integer year) {
        List<PeriodSalesDto> result = new ArrayList<>();
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        // 주차 계산
        WeekFields weekFields = WeekFields.ISO;
        LocalDate currentWeekStart = startOfYear;

        int weekNumber = 1;
        while (currentWeekStart.isBefore(endOfYear) || currentWeekStart.isEqual(endOfYear)) {
            LocalDate weekEnd = currentWeekStart.plusDays(6);
            if (weekEnd.isAfter(endOfYear)) {
                weekEnd = endOfYear;
            }

            Long sales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                    branchId, OrderStatus.CONFIRMED,
                    currentWeekStart.atStartOfDay(), weekEnd.atTime(LocalTime.MAX));

            result.add(PeriodSalesDto.builder()
                    .periodLabel(String.format("Week %d", weekNumber))
                    .sales(sales != null ? sales : 0L)
                    .build());

            currentWeekStart = currentWeekStart.plusWeeks(1);
            weekNumber++;

            // 최대 53주
            if (weekNumber > 53) break;
        }

        return result;
    }

    /**
     * 카테고리별 매출 분석
     */
    public CategorySalesDto getCategorySales(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("카테고리별 매출 조회 시작 - branchId: {}", branchId);

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 상품별 매출 통계 조회
        List<Object[]> productSalesStats = orderedItemRepository.findProductSalesStatistics(
                branchId, startDateTime, endDateTime);

        // 카테고리별 매출 집계 (카테고리 정보가 없으므로 상품별로 처리)
        Map<String, Long> categorySalesDistribution = new LinkedHashMap<>();
        Long totalSales = 0L;
        String topCategory = "없음";
        Long topCategorySales = 0L;

        for (Object[] stat : productSalesStats) {
            String productName = (String) stat[1];
            Long sales = ((Number) stat[3]).longValue();

            categorySalesDistribution.put(productName, sales);
            totalSales += sales;

            if (sales > topCategorySales) {
                topCategory = productName;
                topCategorySales = sales;
            }
        }

        // 상위 10개만 표시
        Map<String, Long> top10Categories = categorySalesDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return CategorySalesDto.builder()
                .categorySalesDistribution(top10Categories)
                .totalSales(totalSales)
                .topCategory(topCategory)
                .topCategorySales(topCategorySales)
                .build();
    }
}

