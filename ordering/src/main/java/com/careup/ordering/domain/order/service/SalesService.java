package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.domain.order.dto.BranchComparisonDto;
import com.careup.ordering.domain.order.dto.NearbyBranchInfoDto;
import com.careup.ordering.domain.order.dto.ProductSalesDto;
import com.careup.ordering.domain.order.dto.SalesForecastDto;
import com.careup.ordering.domain.order.dto.SalesStatisticsDto;
import com.careup.ordering.domain.order.dto.request.SalesStatisticsRequestDto;
import com.careup.ordering.domain.order.dto.response.ProductSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.SalesStatisticsResponseDto;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesService {

    private final OrderRepository orderRepository;
    private final OrderedItemRepository orderedItemRepository;
    private final BranchClient branchClient;

    /**
     * 매출 통계 조회 (요일별, 시간별, 기간별)
     */
    public SalesStatisticsResponseDto getSalesStatistics(SalesStatisticsRequestDto request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByBranchIdAndOrderStatusAndCreatedAtBetween(
                request.getBranchId(), OrderStatus.CONFIRMED, startDateTime, endDateTime);

        List<SalesStatisticsDto> statistics;

        switch (request.getPeriodType().toUpperCase()) {
            case "HOUR":
                statistics = calculateHourlySales(orders);
                break;
            case "DAY_OF_WEEK":
                statistics = calculateDayOfWeekSales(orders);
                break;
            case "DAY":
                statistics = calculateDailySales(orders);
                break;
            case "WEEK":
                statistics = calculateWeeklySales(orders);
                break;
            case "MONTH":
                statistics = calculateMonthlySales(orders);
                break;
            default:
                statistics = calculateDailySales(orders);
        }

        Long totalSales = orders.stream()
                .mapToLong(Order::getTotalAmount)
                .sum();

        return SalesStatisticsResponseDto.builder()
                .branchId(request.getBranchId())
                .periodType(request.getPeriodType())
                .totalSales(totalSales)
                .totalOrders((long) orders.size())
                .statistics(statistics)
                .build();
    }

    /**
     * 시간별 매출 통계
     */
    private List<SalesStatisticsDto> calculateHourlySales(List<Order> orders) {
        Map<Integer, List<Order>> hourlyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().getHour()));

        return hourlyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream()
                            .mapToLong(Order::getTotalAmount)
                            .sum();
                    Long orderCount = (long) entry.getValue().size();

                    return SalesStatisticsDto.builder()
                            .hour(entry.getKey())
                            .totalSales(totalSales)
                            .totalOrders(orderCount)
                            .averageOrderAmount(orderCount > 0 ? totalSales / orderCount : 0L)
                            .period("HOUR")
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto::getHour))
                .collect(Collectors.toList());
    }

    /**
     * 요일별 매출 통계
     */
    private List<SalesStatisticsDto> calculateDayOfWeekSales(List<Order> orders) {
        Map<DayOfWeek, List<Order>> dayOfWeekOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().getDayOfWeek()));

        return dayOfWeekOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream()
                            .mapToLong(Order::getTotalAmount)
                            .sum();
                    Long orderCount = (long) entry.getValue().size();

                    return SalesStatisticsDto.builder()
                            .dayOfWeek(entry.getKey().getDisplayName(TextStyle.FULL, Locale.KOREAN))
                            .totalSales(totalSales)
                            .totalOrders(orderCount)
                            .averageOrderAmount(orderCount > 0 ? totalSales / orderCount : 0L)
                            .period("DAY_OF_WEEK")
                            .build();
                })
                .sorted(Comparator.comparing(dto -> {
                    String day = dto.getDayOfWeek();
                    if (day.equals("월요일")) return DayOfWeek.MONDAY;
                    if (day.equals("화요일")) return DayOfWeek.TUESDAY;
                    if (day.equals("수요일")) return DayOfWeek.WEDNESDAY;
                    if (day.equals("목요일")) return DayOfWeek.THURSDAY;
                    if (day.equals("금요일")) return DayOfWeek.FRIDAY;
                    if (day.equals("토요일")) return DayOfWeek.SATURDAY;
                    return DayOfWeek.SUNDAY;
                }))
                .collect(Collectors.toList());
    }

    /**
     * 일별 매출 통계
     */
    private List<SalesStatisticsDto> calculateDailySales(List<Order> orders) {
        Map<LocalDate, List<Order>> dailyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));

        return dailyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream()
                            .mapToLong(Order::getTotalAmount)
                            .sum();
                    Long orderCount = (long) entry.getValue().size();

                    return SalesStatisticsDto.builder()
                            .date(entry.getKey())
                            .totalSales(totalSales)
                            .totalOrders(orderCount)
                            .averageOrderAmount(orderCount > 0 ? totalSales / orderCount : 0L)
                            .period("DAY")
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 주별 매출 통계
     */
    private List<SalesStatisticsDto> calculateWeeklySales(List<Order> orders) {
        Map<String, List<Order>> weeklyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> {
                    LocalDate date = order.getCreatedAt().toLocalDate();
                    int weekOfYear = date.getDayOfYear() / 7 + 1;
                    return date.getYear() + "-W" + weekOfYear;
                }));

        return weeklyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream()
                            .mapToLong(Order::getTotalAmount)
                            .sum();
                    Long orderCount = (long) entry.getValue().size();
                    LocalDate firstDate = entry.getValue().stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return SalesStatisticsDto.builder()
                            .date(firstDate)
                            .totalSales(totalSales)
                            .totalOrders(orderCount)
                            .averageOrderAmount(orderCount > 0 ? totalSales / orderCount : 0L)
                            .period("WEEK")
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 월별 매출 통계
     */
    private List<SalesStatisticsDto> calculateMonthlySales(List<Order> orders) {
        Map<String, List<Order>> monthlyOrders = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getCreatedAt().getYear() + "-" +
                        String.format("%02d", order.getCreatedAt().getMonthValue())));

        return monthlyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream()
                            .mapToLong(Order::getTotalAmount)
                            .sum();
                    Long orderCount = (long) entry.getValue().size();
                    LocalDate firstDate = entry.getValue().stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return SalesStatisticsDto.builder()
                            .date(firstDate)
                            .totalSales(totalSales)
                            .totalOrders(orderCount)
                            .averageOrderAmount(orderCount > 0 ? totalSales / orderCount : 0L)
                            .period("MONTH")
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 상품별 매출 조회 (마진율 높은 상품, 판매량 많은 상품 등)
     */
    public ProductSalesResponseDto getProductSales(Long branchId, LocalDate startDate, LocalDate endDate, String sortType) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = orderedItemRepository.findProductSalesStatistics(
                branchId, startDateTime, endDateTime);

        List<ProductSalesDto> products = results.stream()
                .map(result -> {
                    Long productId = ((Number) result[0]).longValue();
                    String productName = (String) result[1];
                    Long totalQuantity = ((Number) result[2]).longValue();
                    Long totalSales = ((Number) result[3]).longValue();
                    Long supplyPrice = ((Number) result[4]).longValue();
                    Double avgSellingPrice = ((Number) result[5]).doubleValue();
                    Long orderCount = ((Number) result[6]).longValue();

                    // 마진율 계산: (평균 판매가 - 공급가) / 평균 판매가 * 100
                    Double marginRate = ((avgSellingPrice - supplyPrice) / avgSellingPrice) * 100;

                    return ProductSalesDto.builder()
                            .productId(productId)
                            .productName(productName)
                            .totalQuantity(totalQuantity)
                            .totalSales(totalSales)
                            .supplyPrice(supplyPrice)
                            .averageSellingPrice(avgSellingPrice.longValue())
                            .marginRate(marginRate)
                            .orderCount(orderCount)
                            .build();
                })
                .collect(Collectors.toList());

        // 정렬 타입에 따라 정렬
        switch (sortType.toUpperCase()) {
            case "HIGH_MARGIN":
                products.sort(Comparator.comparing(ProductSalesDto::getMarginRate).reversed());
                break;
            case "LOW_MARGIN":
                products.sort(Comparator.comparing(ProductSalesDto::getMarginRate));
                break;
            case "HIGH_SALES":
                products.sort(Comparator.comparing(ProductSalesDto::getTotalSales).reversed());
                break;
            case "LOW_SALES":
                products.sort(Comparator.comparing(ProductSalesDto::getTotalSales));
                break;
            default:
                products.sort(Comparator.comparing(ProductSalesDto::getTotalSales).reversed());
        }

        return ProductSalesResponseDto.builder()
                .branchId(branchId)
                .sortType(sortType)
                .products(products)
                .build();
    }

    /**
     * 인근 지역 가맹점 평균 및 매출 비교 (위치 기반 - MSA 개선)
     */
    public List<BranchComparisonDto> compareBranchSales(Long branchId, LocalDate startDate, LocalDate endDate, Double radiusKm) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        try {
            // branch 서비스에서 인근 지점 조회
            Map<String, Object> response = branchClient.getNearbyBranches(branchId, radiusKm);

            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) response.get("result");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nearbyBranchesData = (List<Map<String, Object>>) resultMap;

            // 인근 지점 정보 파싱
            List<NearbyBranchInfoDto> nearbyBranches = nearbyBranchesData.stream()
                    .map(data -> NearbyBranchInfoDto.builder()
                            .id(((Number) data.get("id")).longValue())
                            .name((String) data.get("name"))
                            .address((String) data.get("address"))
                            .latitude(data.get("latitude") != null ? ((Number) data.get("latitude")).doubleValue() : null)
                            .longitude(data.get("longitude") != null ? ((Number) data.get("longitude")).doubleValue() : null)
                            .distance(data.get("distance") != null ? ((Number) data.get("distance")).doubleValue() : null)
                            .build())
                    .collect(Collectors.toList());

            // 본인 지점 + 인근 지점 ID 목록
            List<Long> allBranchIds = new ArrayList<>();
            allBranchIds.add(branchId);
            nearbyBranches.forEach(branch -> allBranchIds.add(branch.getId()));

            // Branch 정보 조회
            Map<Long, String> branchNamesMap = getBranchNamesMap(allBranchIds);

            // 각 지점의 매출 통계 계산
            List<BranchComparisonDto> comparisons = new ArrayList<>();

            for (Long targetBranchId : allBranchIds) {
                Long totalSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                        targetBranchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

                Long totalOrders = orderRepository.countOrdersByBranchAndPeriod(
                        targetBranchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

                // 이전 기간 매출 조회 (성장률 계산용)
                LocalDateTime prevStartDateTime = startDateTime.minusDays(endDate.toEpochDay() - startDate.toEpochDay() + 1);
                LocalDateTime prevEndDateTime = startDateTime.minusDays(1);

                Long prevSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                        targetBranchId, OrderStatus.CONFIRMED, prevStartDateTime, prevEndDateTime);

                Double growthRate = prevSales > 0 ?
                        ((double) (totalSales - prevSales) / prevSales) * 100 : 0.0;

                BranchComparisonDto comparison = BranchComparisonDto.builder()
                        .branchId(targetBranchId)
                        .branchName(branchNamesMap.getOrDefault(targetBranchId, "Branch-" + targetBranchId))
                        .totalSales(totalSales)
                        .totalOrders(totalOrders)
                        .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                        .salesGrowthRate(growthRate)
                        .build();

                comparisons.add(comparison);
            }

            return comparisons.stream()
                    .sorted(Comparator.comparing(BranchComparisonDto::getTotalSales).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("인근 지점 조회 실패, 기본 방식으로 처리: {}", e.getMessage());
            // Feign 통신 실패 시 본인 지점만 반환
            return getDefaultBranchComparison(branchId, startDateTime, endDateTime);
        }
    }

    /**
     * Branch 서비스에서 Branch 이름 조회 (여러 개)
     */
    private Map<Long, String> getBranchNamesMap(List<Long> branchIds) {
        try {
            Map<String, Object> response = branchClient.getBranchesByIds(branchIds);

            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) response.get("result");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branchesData = (List<Map<String, Object>>) resultMap;

            return branchesData.stream()
                    .collect(Collectors.toMap(
                            data -> ((Number) data.get("id")).longValue(),
                            data -> (String) data.get("name")
                    ));
        } catch (Exception e) {
            log.error("Branch 이름 조회 실패: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Feign 통신 실패 시 기본 비교 데이터 반환
     */
    private List<BranchComparisonDto> getDefaultBranchComparison(Long branchId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Long totalSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        Long totalOrders = orderRepository.countOrdersByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        BranchComparisonDto comparison = BranchComparisonDto.builder()
                .branchId(branchId)
                .branchName("Branch-" + branchId)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                .salesGrowthRate(0.0)
                .build();

        return Collections.singletonList(comparison);
    }

    /**
     * 소속 가맹점의 예상 매출액 조회
     */
    public SalesForecastDto getSalesForecast(Long branchId, LocalDate targetDate) {
        // 지난 30일 평균 매출 기반 예측
        LocalDate thirtyDaysAgo = targetDate.minusDays(30);
        LocalDateTime startDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime endDateTime = targetDate.minusDays(1).atTime(LocalTime.MAX);

        Long previousPeriodSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 일평균 매출 계산
        Long dailyAverageSales = previousPeriodSales / 30;

        // 요일별 가중치 적용 (실제로는 더 복잡한 예측 모델 사용 가능)
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        double dayWeightFactor = getDayWeightFactor(dayOfWeek);

        Long expectedSales = (long) (dailyAverageSales * dayWeightFactor);

        Double growthRate = previousPeriodSales > 0 ?
                ((double) (expectedSales - dailyAverageSales) / dailyAverageSales) * 100 : 0.0;

        return SalesForecastDto.builder()
                .branchId(branchId)
                .forecastDate(targetDate)
                .expectedSales(expectedSales)
                .previousPeriodSales(previousPeriodSales)
                .growthRate(growthRate)
                .forecastBasis("지난 30일 평균 매출 기반 + 요일별 가중치")
                .build();
    }

    /**
     * 요일별 가중치 반환 (주말이 더 높은 매출을 보인다고 가정)
     */
    private double getDayWeightFactor(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SATURDAY:
            case SUNDAY:
                return 1.3; // 주말 30% 증가
            case FRIDAY:
                return 1.15; // 금요일 15% 증가
            case MONDAY:
                return 0.9; // 월요일 10% 감소
            default:
                return 1.0; // 평일 기본
        }
    }

    /**
     * branch 서비스용 - 예상 매출 계산을 위한 매출 통계 조회
     */
    public Map<String, Object> getSalesStatisticsForForecast(Long branchId, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 총 매출액 조회
        Long totalSales = orderRepository.calculateTotalSalesByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 총 주문 수 조회
        Long totalOrders = orderRepository.countOrdersByBranchAndPeriod(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 일평균 매출 계산
        Long averageDailySales = totalSales / days;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("branchId", branchId);
        statistics.put("totalSales", totalSales);
        statistics.put("totalOrders", totalOrders);
        statistics.put("averageDailySales", averageDailySales);
        statistics.put("days", days);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);

        return statistics;
    }
}
