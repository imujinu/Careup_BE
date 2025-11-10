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
import java.time.format.DateTimeFormatter;
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
    private final DashboardCacheService dashboardCacheService;

    /**
     * 매출 현황 조회 (총 매출, 월간 매출, 총 주문 수, 최근 7일 매출)
     */
    public SalesSummaryDto getSalesSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("[Ordering 매출 현황 조회 시작] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);

        // 1. Redis 캐시 확인
        Map<String, Object> cachedData = dashboardCacheService.getCachedSalesData(branchId);

        // 캐시 상태 로깅
        if (cachedData == null) {
            log.info("[Redis 캐시] cachedData is NULL - 캐시가 완전히 비어있음");
        } else if (cachedData.isEmpty()) {
            log.info("[Redis 캐시] cachedData is EMPTY - 캐시 키는 존재하지만 데이터 없음");
        } else {
            log.info("[Redis 캐시] 캐시 데이터 존재 - 키 개수: {}, 키 목록: {}",
                    cachedData.size(), cachedData.keySet());
            cachedData.forEach((k, v) -> log.debug("  - {}: {}", k, v));
        }

        // 캐시가 있고, totalOrders가 존재하면 캐시 사용 (0 매출도 유효한 데이터)
        if (cachedData != null && !cachedData.isEmpty()) {
            Long cachedTotalSales = getLongValue(cachedData.get("totalSales"));
            Long cachedTotalOrders = getLongValue(cachedData.get("totalOrders"));

            log.info("[Ordering 매출 현황 캐시 확인] branchId={}, cachedTotalSales={}, cachedTotalOrders={}, cached data size={}",
                    branchId, cachedTotalSales, cachedTotalOrders, cachedData.size());

            // totalOrders가 null이 아니면 캐시가 유효함 (매출이 0이어도 주문 수가 있으면 유효)
            if (cachedTotalOrders != null) {
                log.info("[Ordering 매출 현황 캐시 히트 - 유효한 데이터] branchId={}, totalOrders={}", branchId, cachedTotalOrders);

                Long monthlySales = getLongValue(cachedData.get("monthlySales"));

                log.debug("[캐시 데이터] totalSales={}, monthlySales={}, totalOrders={}", cachedTotalSales, monthlySales, cachedTotalOrders);

                // 최근 7일 매출 데이터 추출
                List<DailySalesDto> last7DaysSales = extractLast7DaysSales(cachedData);
                log.debug("[캐시 데이터] last7DaysSales size={}", last7DaysSales != null ? last7DaysSales.size() : 0);

                SalesSummaryDto result = SalesSummaryDto.builder()
                        .totalSales(cachedTotalSales != null ? cachedTotalSales : 0L)
                        .monthlySales(monthlySales != null ? monthlySales : 0L)
                        .totalOrders(cachedTotalOrders)
                        .last7DaysSales(last7DaysSales)
                        .build();
                log.info("[Ordering 매출 현황 조회 완료 - 캐시] branchId={}, totalSales={}, monthlySales={}, totalOrders={}",
                        branchId, result.getTotalSales(), result.getMonthlySales(), result.getTotalOrders());
                return result;
            } else {
                log.warn("[Ordering 매출 현황 캐시 무효 - DB 조회 진행] branchId={}, cachedTotalSales={}, cachedTotalOrders={}",
                        branchId, cachedTotalSales, cachedTotalOrders);
            }
        } else {
            log.info("[Ordering 매출 현황 캐시 미스] branchId={}, cachedData is null or empty", branchId);
        }

        log.info("[Ordering 매출 현황 캐시 미스 - DB 조회 시작] branchId={}", branchId);

        // 2. 캐시에 없으면 DB 조회 (기존 로직)
        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        log.info("[날짜 범위 설정] startDate={}, endDate={}, 현재날짜={}", startDate, endDate, LocalDate.now());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        log.info("[DateTime 범위] startDateTime={}, endDateTime={}", startDateTime, endDateTime);

        // 총 매출 계산
        log.info("[총 매출 계산 시작] branchId={}, status=CONFIRMED, startDateTime={}, endDateTime={}",
                branchId, startDateTime, endDateTime);
        Long totalSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);
        log.info("[총 매출 계산 완료] totalSales={}", totalSales);

        // 해당 기간의 주문 목록 확인 (디버깅용)
        List<Order> debugOrders = orderRepository.findByBranchIdAndOrderStatusAndCreatedAtBetween(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);
        log.info("[디버깅] 조회된 주문 수: {}, 주문 ID 목록: {}", debugOrders.size(),
                debugOrders.stream().limit(10).map(Order::getId).collect(java.util.stream.Collectors.toList()));
        if (!debugOrders.isEmpty()) {
            Order sampleOrder = debugOrders.get(0);
            log.info("[디버깅] 샘플 주문 - ID: {}, branchId: {}, totalAmount: {}, status: {}, createdAt: {}",
                    sampleOrder.getId(), sampleOrder.getBranchId(), sampleOrder.getTotalAmount(),
                    sampleOrder.getOrderStatus(), sampleOrder.getCreatedAt());
        } else {
            log.warn("[디버깅] ⚠️ 조회된 주문이 없습니다! DB에 주문 데이터가 있는지 확인 필요");
            // 전체 주문 수 확인
            long allOrdersCount = orderRepository.count();
            log.warn("[디버깅] 전체 주문 수: {}", allOrdersCount);
            if (allOrdersCount > 0) {
                // 해당 지점의 모든 주문 확인
                List<Order> allBranchOrders = orderRepository.findAll().stream()
                        .filter(o -> o.getBranchId().equals(branchId))
                        .limit(5)
                        .collect(java.util.stream.Collectors.toList());
                log.warn("[디버깅] 지점 {} 전체 주문 샘플 (최대 5개):", branchId);
                allBranchOrders.forEach(o ->
                    log.warn("  - Order ID: {}, branchId: {}, status: {}, amount: {}, createdAt: {}",
                            o.getId(), o.getBranchId(), o.getOrderStatus(), o.getTotalAmount(), o.getCreatedAt())
                );
            }
        }

        // 월간 매출 (현재 월)
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();
        log.info("[월간 매출 계산 시작] monthStart={}, monthEnd={}", monthStart, monthEnd);
        Long monthlySales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED,
                monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
        log.debug("[월간 매출 계산 완료] monthlySales={}", monthlySales);

        // 총 주문 수
        log.debug("[총 주문 수 계산 시작]");
        Long totalOrders = orderRepository.countOrdersByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);
        log.debug("[총 주문 수 계산 완료] totalOrders={}", totalOrders);

        // 최근 7일 매출
        log.debug("[최근 7일 매출 계산 시작]");
        List<DailySalesDto> last7DaysSales = getLast7DaysSales(branchId);
        log.debug("[최근 7일 매출 계산 완료] last7DaysSales size={}", last7DaysSales.size());

        SalesSummaryDto result = SalesSummaryDto.builder()
                .totalSales(totalSales != null ? totalSales : 0L)
                .monthlySales(monthlySales != null ? monthlySales : 0L)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .last7DaysSales(last7DaysSales)
                .build();
        log.info("[Ordering 매출 현황 조회 완료 - DB] branchId={}, totalSales={}, monthlySales={}, totalOrders={}",
                branchId, result.getTotalSales(), result.getMonthlySales(), result.getTotalOrders());
        return result;
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
        log.info("[Ordering 재고 현황 조회 시작] branchId={}", branchId);

        List<BranchProduct> branchProducts = branchProductRepository.findByBranchId(branchId);
        log.debug("[재고 현황] branchId={}, 전체 상품 수={}", branchId, branchProducts.size());

        // 총 재고 품목
        Long totalProducts = (long) branchProducts.size();

        // 재고 부족 품목 (안전재고 이하)
        Long lowStockProducts = branchProducts.stream()
                .filter(bp -> bp.getStockQuantity() <= bp.getSafetystock())
                .count();
        log.debug("[재고 현황] 재고 부족 상품 수={}", lowStockProducts);

        // 재고 충족률 계산
        Double stockFulfillmentRate = totalProducts > 0
                ? ((totalProducts - lowStockProducts) * 100.0 / totalProducts)
                : 100.0;
        log.debug("[재고 현황] 재고 충족률={}%", stockFulfillmentRate);

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
        log.debug("[재고 현황] 재고 알림 수={}", stockAlerts.size());

        InventorySummaryDto result = InventorySummaryDto.builder()
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .stockFulfillmentRate(Math.round(stockFulfillmentRate * 100.0) / 100.0)
                .stockAlerts(stockAlerts)
                .build();
        log.info("[Ordering 재고 현황 조회 완료] branchId={}, totalProducts={}, lowStockProducts={}, stockFulfillmentRate={}",
                branchId, result.getTotalProducts(), result.getLowStockProducts(), result.getStockFulfillmentRate());
        return result;
    }

    /**
     * 주문 현황 조회 (총 주문 수, 처리 완료, 대기 중, 취소)
     */
    public OrderSummaryDto getOrderSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("[Ordering 주문 현황 조회 시작] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);

        // 1. Redis 캐시 확인
        Map<String, Object> cachedData = dashboardCacheService.getCachedOrderData(branchId);
        if (cachedData != null && !cachedData.isEmpty()) {
            log.info("[Ordering 주문 현황 캐시 히트] branchId={}, cached data size={}", branchId, cachedData.size());

            Long totalOrders = getLongValue(cachedData.get("totalOrders"));
            Long confirmedOrders = getLongValue(cachedData.get("status:CONFIRMED"));
            Long pendingOrders = getLongValue(cachedData.get("status:PENDING"));
            Long canceledOrders = getLongValue(cachedData.get("status:CANCELLED"));

            log.debug("[캐시 데이터] totalOrders={}, confirmedOrders={}, pendingOrders={}, canceledOrders={}",
                    totalOrders, confirmedOrders, pendingOrders, canceledOrders);

            Map<String, Long> orderStatusDistribution = new HashMap<>();
            orderStatusDistribution.put("CONFIRMED", confirmedOrders);
            orderStatusDistribution.put("PENDING", pendingOrders);
            orderStatusDistribution.put("CANCELLED", canceledOrders);

            OrderSummaryDto result = OrderSummaryDto.builder()
                    .totalOrders(totalOrders)
                    .completedOrders(confirmedOrders)
                    .pendingOrders(pendingOrders)
                    .canceledOrders(canceledOrders)
                    .orderStatusDistribution(orderStatusDistribution)
                    .build();
            log.info("[Ordering 주문 현황 조회 완료 - 캐시] branchId={}, totalOrders={}, completedOrders={}, pendingOrders={}, canceledOrders={}",
                    branchId, result.getTotalOrders(), result.getCompletedOrders(), result.getPendingOrders(), result.getCanceledOrders());
            return result;
        }

        log.info("[Ordering 주문 현황 캐시 미스 - DB 조회 시작] branchId={}", branchId);

        // 2. 캐시에 없으면 DB 조회 (기존 로직)
        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        log.debug("[날짜 범위 조정] startDate={}, endDate={}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 기간별 주문 조회
        log.debug("[주문 조회 시작] startDateTime={}, endDateTime={}", startDateTime, endDateTime);
        List<Order> allOrders = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime)
                .stream()
                .filter(order -> order.getBranchId().equals(branchId))
                .collect(Collectors.toList());
        log.debug("[주문 조회 완료] 전체 주문 수={}", allOrders.size());

        // 상태별 주문 수 계산
        Map<OrderStatus, Long> statusCount = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));
        log.debug("[상태별 주문 수] statusCount={}", statusCount);

        Long totalOrders = (long) allOrders.size();
        Long completedOrders = statusCount.getOrDefault(OrderStatus.CONFIRMED, 0L);
        Long pendingOrders = statusCount.getOrDefault(OrderStatus.PENDING, 0L);
        Long canceledOrders = statusCount.getOrDefault(OrderStatus.CANCELLED, 0L);

        // 주문 상태 분포 (차트용)
        Map<String, Long> orderStatusDistribution = new HashMap<>();
        orderStatusDistribution.put("CONFIRMED", completedOrders);
        orderStatusDistribution.put("PENDING", pendingOrders);
        orderStatusDistribution.put("CANCELLED", canceledOrders);

        OrderSummaryDto result = OrderSummaryDto.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .canceledOrders(canceledOrders)
                .orderStatusDistribution(orderStatusDistribution)
                .build();
        log.info("[Ordering 주문 현황 조회 완료 - DB] branchId={}, totalOrders={}, completedOrders={}, pendingOrders={}, canceledOrders={}",
                branchId, result.getTotalOrders(), result.getCompletedOrders(), result.getPendingOrders(), result.getCanceledOrders());
        return result;
    }

    /**
     * 기간별 매출 추이 조회 (연간, 월간, 주간)
     */
    public SalesTrendDto getSalesTrend(Long branchId, String period, Integer year) {
        log.info("[Ordering 매출 추이 조회 시작] branchId={}, period={}, year={}", branchId, period, year);

        if (year == null) {
            year = LocalDate.now().getYear();
            log.debug("[매출 추이] year가 null이어서 현재 년도 사용: {}", year);
        }

        List<PeriodSalesDto> salesData;
        Long totalSales = 0L;
        Double yearOverYearGrowth = 0.0;

        switch (period.toUpperCase()) {
            case "YEARLY":
                log.debug("[매출 추이] YEARLY 기간으로 조회");
                salesData = getYearlySalesTrend(branchId, year);
                break;
            case "MONTHLY":
                log.debug("[매출 추이] MONTHLY 기간으로 조회");
                salesData = getMonthlySalesTrend(branchId, year);
                break;
            case "WEEKLY":
                log.debug("[매출 추이] WEEKLY 기간으로 조회");
                salesData = getWeeklySalesTrend(branchId, year);
                break;
            default:
                log.debug("[매출 추이] 기본값 MONTHLY로 조회");
                salesData = getMonthlySalesTrend(branchId, year);
        }

        log.debug("[매출 추이] salesData size={}", salesData != null ? salesData.size() : 0);

        // 총 매출 계산
        totalSales = salesData.stream()
                .mapToLong(PeriodSalesDto::getSales)
                .sum();
        log.debug("[매출 추이] totalSales={}", totalSales);

        // 전년 대비 증감률 계산 (간단히 0으로 설정, 실제 구현 시 전년도 데이터 필요)
        yearOverYearGrowth = 0.0;

        // 목표 달성률 (간단히 100으로 설정, 실제 구현 시 목표 데이터 필요)
        Double goalAchievementRate = 100.0;

        SalesTrendDto result = SalesTrendDto.builder()
                .period(period)
                .salesData(salesData)
                .totalSales(totalSales)
                .yearOverYearGrowth(yearOverYearGrowth)
                .goalAchievementRate(goalAchievementRate)
                .build();
        log.info("[Ordering 매출 추이 조회 완료] branchId={}, period={}, totalSales={}, salesData size={}",
                branchId, period, result.getTotalSales(), result.getSalesData() != null ? result.getSalesData().size() : 0);
        return result;
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
     * 올해 전체 주차를 계산하되, 최근 7주만 반환
     * periodLabel: 최근 7주는 "MM-DD~MM-DD" 형식
     */
    private List<PeriodSalesDto> getWeeklySalesTrend(Long branchId, Integer year) {
        List<PeriodSalesDto> allWeeks = new ArrayList<>();
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        LocalDate today = LocalDate.now();

        // 올해가 아닌 경우 연말까지, 올해인 경우 오늘까지
        LocalDate calculationEndDate = (year < today.getYear()) ? endOfYear : today;

        // 주차 계산
        LocalDate currentWeekStart = startOfYear;
        int weekNumber = 1;

        while (currentWeekStart.isBefore(calculationEndDate) || currentWeekStart.isEqual(calculationEndDate)) {
            LocalDate weekEnd = currentWeekStart.plusDays(6);
            if (weekEnd.isAfter(calculationEndDate)) {
                weekEnd = calculationEndDate;
            }

            Long sales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                    branchId, OrderStatus.CONFIRMED,
                    currentWeekStart.atStartOfDay(), weekEnd.atTime(LocalTime.MAX));

            // 날짜 형식: MM-DD~MM-DD
            String periodLabel = String.format("%02d-%02d~%02d-%02d",
                    currentWeekStart.getMonthValue(), currentWeekStart.getDayOfMonth(),
                    weekEnd.getMonthValue(), weekEnd.getDayOfMonth());

            allWeeks.add(PeriodSalesDto.builder()
                    .periodLabel(periodLabel)
                    .sales(sales != null ? sales : 0L)
                    .build());

            currentWeekStart = currentWeekStart.plusWeeks(1);
            weekNumber++;

            // 최대 53주
            if (weekNumber > 53) break;
        }

        // 최근 7주만 반환
        int totalWeeks = allWeeks.size();
        if (totalWeeks > 7) {
            log.debug("[주별 매출 추이] 전체 {}주 중 최근 7주만 반환", totalWeeks);
            return allWeeks.subList(totalWeeks - 7, totalWeeks);
        }

        log.debug("[주별 매출 추이] 총 {}주 데이터 반환", totalWeeks);
        return allWeeks;
    }

    /**
     * 카테고리별 매출 분석
     */
    public CategorySalesDto getCategorySales(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.info("[Ordering 카테고리별 매출 조회 시작] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);

        // 기본값 설정
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        log.debug("[Ordering 카테고리별 매출] 조정된 날짜 범위 - startDate={}, endDate={}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 카테고리별 매출 통계 조회
        List<Object[]> categorySalesStats = orderedItemRepository.findBranchCategorySalesStatistics(
                branchId, startDateTime, endDateTime);

        log.debug("[Ordering 카테고리별 매출] 조회된 통계 수: {}", categorySalesStats.size());

        // 카테고리별 매출 집계
        Map<String, Long> categorySalesDistribution = new LinkedHashMap<>();
        Long totalSales = 0L;
        String topCategory = "없음";
        Long topCategorySales = 0L;

        for (Object[] stat : categorySalesStats) {
            // [categoryId, categoryName, totalSales, totalQuantity, orderCount]
            Long categoryId = ((Number) stat[0]).longValue();
            String categoryName = (String) stat[1];
            Long sales = ((Number) stat[2]).longValue();
            
            log.debug("[Ordering 카테고리별 매출] categoryId={}, categoryName={}, sales={}", categoryId, categoryName, sales);

            // 카테고리가 없는 경우 "미분류"로 처리
            if (categoryName == null || categoryName.isEmpty()) {
                categoryName = "미분류";
            }

            // 이미 같은 카테고리가 있으면 합산
            categorySalesDistribution.put(categoryName, 
                    categorySalesDistribution.getOrDefault(categoryName, 0L) + sales);
            totalSales += sales;

            if (sales > topCategorySales) {
                topCategory = categoryName;
                topCategorySales = sales;
            }
        }

        log.info("[Ordering 카테고리별 매출] 전체 카테고리 수: {}, 총 매출: {}, 최고 카테고리: {}",
                categorySalesDistribution.size(), totalSales, topCategory);

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

        CategorySalesDto result = CategorySalesDto.builder()
                .categorySalesDistribution(top10Categories)
                .totalSales(totalSales)
                .topCategory(topCategory)
                .topCategorySales(topCategorySales)
                .build();

        log.info("[Ordering 카테고리별 매출 조회 완료] branchId={}, totalSales={}, categories={}",
                branchId, result.getTotalSales(), result.getCategorySalesDistribution().keySet());

        return result;
    }

    // ========== Helper Methods ==========

    /**
     * Object를 Long으로 안전하게 변환
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Long 변환 실패: {}", value);
            return 0L;
        }
    }

    /**
     * Redis 캐시에서 최근 7일 매출 데이터 추출
     */
    private List<DailySalesDto> extractLast7DaysSales(Map<String, Object> cachedData) {
        List<DailySalesDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dailyKey = "daily:" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Long sales = getLongValue(cachedData.get(dailyKey));

            result.add(DailySalesDto.builder()
                    .date(date)
                    .sales(sales)
                    .build());
        }

        return result;
    }
}
