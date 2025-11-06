package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.BranchSalesDetailDto;
import com.careup.ordering.domain.order.dto.ProductSalesDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import com.careup.ordering.domain.order.dto.response.TopBranchResponseDto;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HqSalesService {

    private final OrderRepository orderRepository;
    private final OrderedItemRepository orderedItemRepository;
    private final ProductRepository productRepository;
    private final BranchClient branchClient;

    /**
     * 전체 지점 매출 내역 기간별 조회
     */
    public AllBranchesSalesResponseDto getAllBranchesSales(HqSalesRequestDto request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        // 정해진 기간 내 전체 지점 주문 조회
        List<Order> orders = orderRepository.findAllByOrderStatusAndCreatedAtBetween(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 기간별 통계 계산 -> 일별(DAY), 주별(WEEK), 월별(MONTH)
        List<AllBranchesSalesDto> salesData;
        String periodType = request.getPeriodType() != null ? request.getPeriodType() : "DAY";

        switch (periodType.toUpperCase()) {
            case "WEEK":
                salesData = calculateAllBranchesWeeklySales(orders);
                break;
            case "MONTH":
                salesData = calculateAllBranchesMonthlySales(orders);
                break;
            default:
                salesData = calculateAllBranchesDailySales(orders);
        }

        // 전체 통계
        Long totalSales = orders.stream().mapToLong(Order::getTotalAmount).sum();
        Long totalOrders = (long) orders.size();

        // 총 지점 수 (활성 지점)
        Integer totalBranchCount = orderRepository.countActiveBranches(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        return AllBranchesSalesResponseDto.builder()
                .periodType(periodType)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalBranchCount(totalBranchCount)
                .salesData(salesData)
                .build();
    }

    /**
     * 전체 지점 대상 기간 내 상품별 판매 통계 조회 (정렬/상위 N개)
     * sortType: HIGH_SALES | LOW_SALES | HIGH_MARGIN | LOW_MARGIN
     */
    public List<ProductSalesDto> getHqProductSales(LocalDate startDate, LocalDate endDate, String sortType, Integer size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = orderedItemRepository.findHqProductSalesStatistics(startDateTime, endDateTime);

        // 판매 발생한 상품들 집계
        Map<Long, ProductSalesDto> productMap = results.stream()
                .map(result -> {
                    Long productId = ((Number) result[0]).longValue();
                    String productName = (String) result[1];
                    Long totalQuantity = ((Number) result[2]).longValue();
                    Long totalSales = ((Number) result[3]).longValue();
                    Long supplyPrice = ((Number) result[4]).longValue();
                    Double avgSellingPrice = ((Number) result[5]).doubleValue();
                    Long orderCount = ((Number) result[6]).longValue();

                    Double marginRate = (avgSellingPrice != 0)
                            ? ((avgSellingPrice - supplyPrice) / avgSellingPrice) * 100
                            : 0.0;

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
                .collect(Collectors.toMap(ProductSalesDto::getProductId, dto -> dto));

        // 전체 상품 목록을 가져와서, 집계에 없는 상품은 0으로 채워 넣음 (판매 0건 포함)
        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            if (!productMap.containsKey(p.getId())) {
                productMap.put(p.getId(), ProductSalesDto.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .totalQuantity(0L)
                        .totalSales(0L)
                        .supplyPrice(p.getSupplyPrice())
                        .averageSellingPrice(0L)
                        .marginRate(0.0)
                        .orderCount(0L)
                        .build());
            }
        }

        List<ProductSalesDto> products = new ArrayList<>(productMap.values());

        String sort = (sortType != null ? sortType : "HIGH_SALES").toUpperCase();
        switch (sort) {
            case "HIGH_MARGIN":
                products.sort(Comparator.comparing(ProductSalesDto::getMarginRate).reversed());
                break;
            case "LOW_MARGIN":
                products.sort(Comparator.comparing(ProductSalesDto::getMarginRate));
                break;
            case "LOW_SALES":
                products.sort(Comparator.comparing(ProductSalesDto::getTotalSales));
                break;
            case "HIGH_SALES":
            default:
                products.sort(Comparator.comparing(ProductSalesDto::getTotalSales).reversed());
        }

        if (size != null && size > 0 && products.size() > size) {
            return products.subList(0, size);
        }
        return products;
    }

    /**
     * 일별(DAY) 전체 지점 매출 통계
     */
    private List<AllBranchesSalesDto> calculateAllBranchesDailySales(List<Order> orders) {
        // 일자별 주문 그룹화
        Map<LocalDate, List<Order>> dailyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));

        return dailyOrders.entrySet().stream()
                .map(entry -> {
                    List<Order> dayOrders = entry.getValue();
                    Long totalSales = dayOrders.stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) dayOrders.size();

                    // 해당 일의 활성 지점 수
                    int activeBranchCount = (int) dayOrders.stream()
                            .map(Order::getBranchId)
                            .distinct()
                            .count();

                    return AllBranchesSalesDto.builder()
                            .date(entry.getKey())
                            .period("DAY")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .activeBranchCount(activeBranchCount)
                            .averageSalesPerBranch(activeBranchCount > 0 ? totalSales / activeBranchCount : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(AllBranchesSalesDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 주별 전체 지점 매출 통계
     */
    private List<AllBranchesSalesDto> calculateAllBranchesWeeklySales(List<Order> orders) {
        Map<String, List<Order>> weeklyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> {
                    LocalDate date = order.getCreatedAt().toLocalDate();
                    int weekOfYear = date.getDayOfYear() / 7 + 1;
                    return date.getYear() + "-W" + weekOfYear;
                }));

        return weeklyOrders.entrySet().stream()
                .map(entry -> {
                    List<Order> weekOrders = entry.getValue();
                    Long totalSales = weekOrders.stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) weekOrders.size();

                    int activeBranchCount = (int) weekOrders.stream()
                            .map(Order::getBranchId)
                            .distinct()
                            .count();

                    LocalDate firstDate = weekOrders.stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return AllBranchesSalesDto.builder()
                            .date(firstDate)
                            .period("WEEK")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .activeBranchCount(activeBranchCount)
                            .averageSalesPerBranch(activeBranchCount > 0 ? totalSales / activeBranchCount : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(AllBranchesSalesDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 월별(MONTH) 전체 지점 매출 통계
     */
    private List<AllBranchesSalesDto> calculateAllBranchesMonthlySales(List<Order> orders) {
        // 월별 주문 그룹화
        Map<String, List<Order>> monthlyOrders = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getCreatedAt().getYear() + "-" +
                        String.format("%02d", order.getCreatedAt().getMonthValue())));

        return monthlyOrders.entrySet().stream()
                .map(entry -> {
                    List<Order> monthOrders = entry.getValue();
                    Long totalSales = monthOrders.stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) monthOrders.size();

                    int activeBranchCount = (int) monthOrders.stream()
                            .map(Order::getBranchId)
                            .distinct()
                            .count();

                    LocalDate firstDate = monthOrders.stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return AllBranchesSalesDto.builder()
                            .date(firstDate)
                            .period("MONTH")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .activeBranchCount(activeBranchCount)
                            .averageSalesPerBranch(activeBranchCount > 0 ? totalSales / activeBranchCount : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(AllBranchesSalesDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 선택한 가맹점의 매출 내역 기간별 조회
     */
    public BranchSalesDetailResponseDto getBranchSalesDetail(Long branchId, HqSalesRequestDto request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        // 해당 지점 주문 조회
        List<Order> branchOrders = orderRepository.findByBranchIdAndOrderStatusAndCreatedAtBetween(
                branchId, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 전체 매출 (점유율 계산용)
        Long totalAllSales = orderRepository.calculateTotalSalesAllBranches(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 지점명 조회
        String branchName = getBranchNameSafe(branchId);

        // 기간별 상세 데이터
        List<BranchSalesDetailDto> salesData;
        String periodType = request.getPeriodType() != null ? request.getPeriodType() : "DAY";

        switch (periodType.toUpperCase()) {
            case "WEEK":
                salesData = calculateBranchWeeklySales(branchId, branchName, branchOrders);
                break;
            case "MONTH":
                salesData = calculateBranchMonthlySales(branchId, branchName, branchOrders);
                break;
            default:
                salesData = calculateBranchDailySales(branchId, branchName, branchOrders);
        }

        // 전체 통계
        Long totalSales = branchOrders.stream().mapToLong(Order::getTotalAmount).sum();
        Long totalOrders = (long) branchOrders.size();
        Double marketShare = totalAllSales > 0 ? (totalSales.doubleValue() / totalAllSales) * 100 : 0.0;

        // 순위 계산
        List<Object[]> allBranchStats = orderRepository.findBranchSalesStatistics(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        Integer ranking = calculateRanking(branchId, allBranchStats);

        return BranchSalesDetailResponseDto.builder()
                .branchId(branchId)
                .branchName(branchName)
                .periodType(periodType)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .marketShare(marketShare)
                .ranking(ranking)
                .salesData(salesData)
                .build();
    }

    /**
     * 일별 지점 매출 상세
     */
    private List<BranchSalesDetailDto> calculateBranchDailySales(Long branchId, String branchName, List<Order> orders) {
        Map<LocalDate, List<Order>> dailyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));

        return dailyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) entry.getValue().size();

                    return BranchSalesDetailDto.builder()
                            .branchId(branchId)
                            .branchName(branchName)
                            .date(entry.getKey())
                            .period("DAY")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(BranchSalesDetailDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 주별 지점 매출 상세
     */
    private List<BranchSalesDetailDto> calculateBranchWeeklySales(Long branchId, String branchName, List<Order> orders) {
        Map<String, List<Order>> weeklyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> {
                    LocalDate date = order.getCreatedAt().toLocalDate();
                    int weekOfYear = date.getDayOfYear() / 7 + 1;
                    return date.getYear() + "-W" + weekOfYear;
                }));

        return weeklyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) entry.getValue().size();
                    LocalDate firstDate = entry.getValue().stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return BranchSalesDetailDto.builder()
                            .branchId(branchId)
                            .branchName(branchName)
                            .date(firstDate)
                            .period("WEEK")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(BranchSalesDetailDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 월별 지점 매출 상세
     */
    private List<BranchSalesDetailDto> calculateBranchMonthlySales(Long branchId, String branchName, List<Order> orders) {
        Map<String, List<Order>> monthlyOrders = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getCreatedAt().getYear() + "-" +
                        String.format("%02d", order.getCreatedAt().getMonthValue())));

        return monthlyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) entry.getValue().size();
                    LocalDate firstDate = entry.getValue().stream()
                            .map(order -> order.getCreatedAt().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    return BranchSalesDetailDto.builder()
                            .branchId(branchId)
                            .branchName(branchName)
                            .date(firstDate)
                            .period("MONTH")
                            .totalSales(totalSales)
                            .totalOrders(totalOrders)
                            .averageOrderAmount(totalOrders > 0 ? totalSales / totalOrders : 0L)
                            .build();
                })
                .sorted(Comparator.comparing(BranchSalesDetailDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 가맹점 간 매출 비교
     */
    public BranchComparisonResponseDto compareBranchesSales(HqSalesRequestDto request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        // 비교할 지점 ID 목록
        List<Long> branchIds = request.getBranchIds();
        if (branchIds == null || branchIds.isEmpty()) {
            throw new IllegalArgumentException("비교할 지점을 선택해주세요.");
        }

        // 지점명 조회
        Map<Long, String> branchNamesMap = getBranchNamesMap(branchIds);

        // 지점별 매출 비교 데이터 조회
        List<Object[]> comparisonStats = orderRepository.findBranchSalesComparison(
                branchIds, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 전체 매출 (선택한 지점들)
        Long totalSales = comparisonStats.stream()
                .mapToLong(stat -> ((Number) stat[1]).longValue())
                .sum();

        // 비교 데이터 생성
        List<BranchSalesDetailDto> comparisonData = new ArrayList<>();

        int ranking = 1;
        for (Object[] stat : comparisonStats) {
            Long branchId = ((Number) stat[0]).longValue();
            Long sales = ((Number) stat[1]).longValue();
            Long orders = ((Number) stat[2]).longValue();

            String branchName = branchNamesMap.getOrDefault(branchId, "Branch-" + branchId);

            Double marketShare = totalSales > 0 ? (sales.doubleValue() / totalSales) * 100 : 0.0;

            BranchSalesDetailDto dto = BranchSalesDetailDto.builder()
                    .branchId(branchId)
                    .branchName(branchName)
                    .date(request.getStartDate())
                    .period(request.getPeriodType() != null ? request.getPeriodType() : "DAY")
                    .totalSales(sales)
                    .totalOrders(orders)
                    .averageOrderAmount(orders > 0 ? sales / orders : 0L)
                    .marketShare(marketShare)
                    .ranking(ranking++)
                    .build();

            comparisonData.add(dto);
        }

        return BranchComparisonResponseDto.builder()
                .periodType(request.getPeriodType() != null ? request.getPeriodType() : "DAY")
                .branchIds(branchIds)
                .totalSales(totalSales)
                .branchNames(branchNamesMap)
                .comparisonData(comparisonData)
                .build();
    }

    /**
     * 이달의 우수 지점 (지정 month의 총 매출 1위 지점)
     * @param year 연도 (null이면 현재 연도)
     * @param month 월 (1-12, null이면 현재 월)
     */
    public TopBranchResponseDto getTopBranchOfMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year != null ? year : now.getYear());
        int m = (month != null ? month : now.getMonthValue());

        LocalDate startDate = LocalDate.of(y, m, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 모든 지점의 매출 통계 [branchId, totalSales, totalOrders]
        List<Object[]> allBranchStats = orderRepository.findBranchSalesStatistics(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        if (allBranchStats == null || allBranchStats.isEmpty()) {
            return TopBranchResponseDto.builder()
                    .branchId(null)
                    .branchName(null)
                    .totalSales(0L)
                    .totalOrders(0L)
                    .month(String.format("%04d-%02d", y, m))
                    .build();
        }

        // 첫 번째가 1위라고 가정 (정렬되어 있지 않다면 정렬)
        allBranchStats.sort((a, b) -> Long.compare(((Number) b[1]).longValue(), ((Number) a[1]).longValue()));
        Object[] top = allBranchStats.get(0);
        Long branchId = ((Number) top[0]).longValue();
        Long totalSales = ((Number) top[1]).longValue();
        Long totalOrders = ((Number) top[2]).longValue();

        String branchName = getBranchName(branchId);

        return TopBranchResponseDto.builder()
                .branchId(branchId)
                .branchName(branchName)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .month(String.format("%04d-%02d", y, m))
                .build();
    }

    /**
     * 지점 순위 계산
     */
    private Integer calculateRanking(Long branchId, List<Object[]> allBranchStats) {
        for (int i = 0; i < allBranchStats.size(); i++) {
            Long currentBranchId = ((Number) allBranchStats.get(i)[0]).longValue();
            if (currentBranchId.equals(branchId)) {
                return i + 1;
            }
        }
        return allBranchStats.size() + 1;
    }

    /**
     * Branch 서비스에서 Branch 이름 조회 (단일) - 안전한 버전
     */
    private String getBranchNameSafe(Long branchId) {
        Map<Long, String> namesMap = getBranchNamesMap(Collections.singletonList(branchId));
        return namesMap.getOrDefault(branchId, "Branch-" + branchId);
    }

    /**
     * Branch 서비스에서 Branch 이름 조회 (여러 개)
     */
    private Map<Long, String> getBranchNamesMap(List<Long> branchIds) {
        try {
            log.info("Branch 이름 조회 시작 - Branch IDs: {}", branchIds);
            Map<String, Object> response = branchClient.getBranchesByIds(branchIds);
            log.info("Branch 서비스 응답: {}", response);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branchesData = (List<Map<String, Object>>) response.get("result");

            if (branchesData == null || branchesData.isEmpty()) {
                log.warn("Branch 데이터가 비어있습니다. 응답: {}", response);
                return new HashMap<>();
            }

            Map<Long, String> branchNamesMap = branchesData.stream()
                    .collect(Collectors.toMap(
                            data -> ((Number) data.get("id")).longValue(),
                            data -> (String) data.get("name")
                    ));

            log.info("Branch 이름 매핑 완료: {}", branchNamesMap);
            return branchNamesMap;
        } catch (Exception e) {
            log.error("Branch 이름 조회 실패: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}
