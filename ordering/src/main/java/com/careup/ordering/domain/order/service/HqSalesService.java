package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.BranchSalesDetailDto;
import com.careup.ordering.domain.order.dto.ProductSalesDto;
import com.careup.ordering.domain.order.dto.CategorySalesDto;
import com.careup.ordering.domain.order.dto.GrowthRateDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import com.careup.ordering.domain.order.dto.response.TopBranchResponseDto;
import com.careup.ordering.domain.order.dto.response.CategorySalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesSummaryResponseDto;
import com.careup.ordering.domain.order.dto.BranchSalesSummaryDto;
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
import java.util.AbstractMap;
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

        // 전일 대비 증가율 계산
        GrowthRateDto growthRate = calculateGrowthRate(request.getStartDate(), request.getEndDate(),
                                                        totalSales, totalOrders, periodType);

        return AllBranchesSalesResponseDto.builder()
                .periodType(periodType)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalBranchCount(totalBranchCount)
                .salesData(salesData)
                .totalSalesGrowth(growthRate.getTotalSalesGrowth())
                .monthlySalesGrowth(growthRate.getMonthlySalesGrowth())
                .totalOrdersGrowth(growthRate.getTotalOrdersGrowth())
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
        String branchName = getBranchName(branchId);

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

        // 지점별 매출 비교 데이터 조회
        List<Object[]> comparisonStats = orderRepository.findBranchSalesComparison(
                branchIds, OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 전체 매출 (선택한 지점들)
        Long totalSales = comparisonStats.stream()
                .mapToLong(stat -> ((Number) stat[1]).longValue())
                .sum();

        // 실제 지점명 조회 (Branch 서비스에서)
        Map<Long, String> branchNames = getBranchNamesMap(branchIds);
        log.info("지점명 조회 결과 - branchIds: {}, branchNames: {}", branchIds, branchNames);

        // 비교 데이터 생성
        List<BranchSalesDetailDto> comparisonData = new ArrayList<>();

        int ranking = 1;
        for (Object[] stat : comparisonStats) {
            Long branchId = ((Number) stat[0]).longValue();
            Long sales = ((Number) stat[1]).longValue();
            Long orders = ((Number) stat[2]).longValue();

            // 실제 지점명 사용, 조회 실패 시 기본값
            String branchName = branchNames.getOrDefault(branchId, "Branch-" + branchId);

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
                .branchNames(branchNames)
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

        // 매출 데이터를 Map으로 변환 (branchId -> {totalSales, totalOrders})
        Map<Long, Long[]> salesMap = new HashMap<>();
        if (allBranchStats != null) {
            for (Object[] stat : allBranchStats) {
                Long branchId = ((Number) stat[0]).longValue();
                Long totalSales = ((Number) stat[1]).longValue();
                Long totalOrders = ((Number) stat[2]).longValue();
                salesMap.put(branchId, new Long[]{totalSales, totalOrders});
            }
        }

        // 모든 지점 목록 조회 (매출 0원인 지점도 포함하기 위해)
        Set<Long> branchIdsFromStats = salesMap.keySet();
        
        Map<Long, String> allBranchNames = new HashMap<>();
        try {
            // 매출 통계에 있는 지점 먼저 조회
            if (!branchIdsFromStats.isEmpty()) {
                Map<String, Object> branchResponse = branchClient.getBranchesByIds(new ArrayList<>(branchIdsFromStats));
                log.debug("우수지점 - 매출 통계 지점 조회 응답: {}", branchResponse);
                
                Object resultObj = branchResponse.get("result");
                List<Map<String, Object>> branchesData = new ArrayList<>();
                
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                    branchesData = list;
                } else if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    Object dataObj = resultMap.get("data");
                    if (dataObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                        branchesData = list;
                    }
                }
                
                if (branchesData != null && !branchesData.isEmpty()) {
                    for (Map<String, Object> branch : branchesData) {
                        try {
                            Object idObj = branch.get("id");
                            Object nameObj = branch.get("name");
                            
                            if (idObj != null && nameObj != null) {
                                Long branchId = ((Number) idObj).longValue();
                                String branchName = String.valueOf(nameObj);
                                allBranchNames.put(branchId, branchName);
                                log.debug("지점 정보 추가: branchId={}, branchName={}", branchId, branchName);
                            }
                        } catch (Exception e) {
                            log.warn("지점 정보 파싱 실패: {}", e.getMessage());
                        }
                    }
                }
            }
            
            // 빈 리스트로 호출해서 모든 지점 목록 시도 (실패해도 무시)
            try {
                Map<String, Object> allBranchResponse = branchClient.getBranchesByIds(Collections.emptyList());
                log.debug("우수지점 - 모든 지점 조회 응답: {}", allBranchResponse);
                
                Object resultObj = allBranchResponse.get("result");
                List<Map<String, Object>> allBranchesData = new ArrayList<>();
                
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                    allBranchesData = list;
                } else if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    Object dataObj = resultMap.get("data");
                    if (dataObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                        allBranchesData = list;
                    }
                }
                
                if (allBranchesData != null && !allBranchesData.isEmpty()) {
                    for (Map<String, Object> branch : allBranchesData) {
                        try {
                            Object idObj = branch.get("id");
                            Object nameObj = branch.get("name");
                            
                            if (idObj != null && nameObj != null) {
                                Long branchId = ((Number) idObj).longValue();
                                String branchName = String.valueOf(nameObj);
                                allBranchNames.put(branchId, branchName);
                                log.debug("모든 지점에서 추가: branchId={}, branchName={}", branchId, branchName);
                            }
                        } catch (Exception e) {
                            log.warn("모든 지점 정보 파싱 실패: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("빈 리스트로 모든 지점 조회 시도 실패 (무시): {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("지점 목록 조회 실패: {}", e.getMessage(), e);
        }
        
        log.info("최종 수집된 지점 이름 개수: {}, 지점 ID들: {}", allBranchNames.size(), allBranchNames.keySet());

        // 모든 지점을 포함한 통계 리스트 생성 (매출이 없는 지점은 0원으로 처리)
        List<Map.Entry<Long, Long[]>> allBranchSales = new ArrayList<>();
        
        // 매출이 있는 지점
        for (Map.Entry<Long, Long[]> entry : salesMap.entrySet()) {
            allBranchSales.add(entry);
        }
        
        // 매출이 없는 지점 (0원으로 추가)
        for (Map.Entry<Long, String> branchEntry : allBranchNames.entrySet()) {
            if (!salesMap.containsKey(branchEntry.getKey())) {
                allBranchSales.add(new AbstractMap.SimpleEntry<>(branchEntry.getKey(), new Long[]{0L, 0L}));
            }
        }

        if (allBranchSales.isEmpty()) {
            return TopBranchResponseDto.builder()
                    .branchId(null)
                    .branchName(null)
                    .totalSales(0L)
                    .totalOrders(0L)
                    .month(String.format("%04d-%02d", y, m))
                    .build();
        }

        // 매출이 가장 높은 지점 찾기 (내림차순 정렬)
        allBranchSales.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        Map.Entry<Long, Long[]> top = allBranchSales.get(0);
        Long branchId = top.getKey();
        Long totalSales = top.getValue()[0];
        Long totalOrders = top.getValue()[1];

        // 평균 매출 계산
        Long totalAllSales = allBranchSales.stream()
                .mapToLong(entry -> entry.getValue()[0])
                .sum();
        Long averageSales = allBranchSales.isEmpty() ? 0L : totalAllSales / allBranchSales.size();
        Long differenceFromAverage = totalSales - averageSales;

        // 브랜치네임이 없으면 다시 조회 시도
        String branchName = allBranchNames.getOrDefault(branchId, null);
        if (branchName == null) {
            log.warn("브랜치네임이 없어서 재조회 시도: branchId={}", branchId);
            branchName = getBranchName(branchId);
            if (branchName == null || branchName.equals("Branch-" + branchId)) {
                log.error("브랜치네임 조회 실패: branchId={}", branchId);
            }
        }

        return TopBranchResponseDto.builder()
                .branchId(branchId)
                .branchName(branchName != null ? branchName : "Branch-" + branchId)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .month(String.format("%04d-%02d", y, m))
                .averageSales(averageSales)
                .differenceFromAverage(differenceFromAverage)
                .build();
    }

    /**
     * 이달의 매출 저조 지점 (지정 month의 총 매출 최하위 지점)
     * @param year 연도 (null이면 현재 연도)
     * @param month 월 (1-12, null이면 현재 월)
     */
    public TopBranchResponseDto getLowBranchOfMonth(Integer year, Integer month) {
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

        // 매출 데이터를 Map으로 변환 (branchId -> {totalSales, totalOrders})
        Map<Long, Long[]> salesMap = new HashMap<>();
        if (allBranchStats != null) {
            for (Object[] stat : allBranchStats) {
                Long branchId = ((Number) stat[0]).longValue();
                Long totalSales = ((Number) stat[1]).longValue();
                Long totalOrders = ((Number) stat[2]).longValue();
                salesMap.put(branchId, new Long[]{totalSales, totalOrders});
            }
        }

        // 모든 지점 목록 조회 (매출 0원인 지점도 포함하기 위해)
        // 먼저 매출 통계에 있는 지점 ID들을 수집
        Set<Long> branchIdsFromStats = salesMap.keySet();
        
        Map<Long, String> allBranchNames = new HashMap<>();
        try {
            // 매출 통계에 있는 지점 먼저 조회
            if (!branchIdsFromStats.isEmpty()) {
                Map<String, Object> branchResponse = branchClient.getBranchesByIds(new ArrayList<>(branchIdsFromStats));
                log.debug("저조지점 - 매출 통계 지점 조회 응답: {}", branchResponse);
                
                Object resultObj = branchResponse.get("result");
                List<Map<String, Object>> branchesData = new ArrayList<>();
                
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                    branchesData = list;
                } else if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    Object dataObj = resultMap.get("data");
                    if (dataObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                        branchesData = list;
                    }
                }
                
                if (branchesData != null && !branchesData.isEmpty()) {
                    for (Map<String, Object> branch : branchesData) {
                        try {
                            Object idObj = branch.get("id");
                            Object nameObj = branch.get("name");
                            
                            if (idObj != null && nameObj != null) {
                                Long branchId = ((Number) idObj).longValue();
                                String branchName = String.valueOf(nameObj);
                                allBranchNames.put(branchId, branchName);
                                log.debug("지점 정보 추가: branchId={}, branchName={}", branchId, branchName);
                            }
                        } catch (Exception e) {
                            log.warn("지점 정보 파싱 실패: {}", e.getMessage());
                        }
                    }
                }
            }
            
            // 빈 리스트로 호출해서 모든 지점 목록 시도 (실패해도 무시)
            try {
                Map<String, Object> allBranchResponse = branchClient.getBranchesByIds(Collections.emptyList());
                log.debug("저조지점 - 모든 지점 조회 응답: {}", allBranchResponse);
                
                Object resultObj = allBranchResponse.get("result");
                List<Map<String, Object>> allBranchesData = new ArrayList<>();
                
                if (resultObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                    allBranchesData = list;
                } else if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    Object dataObj = resultMap.get("data");
                    if (dataObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                        allBranchesData = list;
                    }
                }
                
                if (allBranchesData != null && !allBranchesData.isEmpty()) {
                    for (Map<String, Object> branch : allBranchesData) {
                        try {
                            Object idObj = branch.get("id");
                            Object nameObj = branch.get("name");
                            
                            if (idObj != null && nameObj != null) {
                                Long branchId = ((Number) idObj).longValue();
                                String branchName = String.valueOf(nameObj);
                                allBranchNames.put(branchId, branchName);
                                log.debug("모든 지점에서 추가: branchId={}, branchName={}", branchId, branchName);
                            }
                        } catch (Exception e) {
                            log.warn("모든 지점 정보 파싱 실패: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("빈 리스트로 모든 지점 조회 시도 실패 (무시): {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("지점 목록 조회 실패: {}", e.getMessage(), e);
        }
        
        log.info("최종 수집된 지점 이름 개수: {}, 지점 ID들: {}", allBranchNames.size(), allBranchNames.keySet());

        // 모든 지점을 포함한 통계 리스트 생성 (매출이 없는 지점은 0원으로 처리)
        List<Map.Entry<Long, Long[]>> allBranchSales = new ArrayList<>();
        
        // 매출이 있는 지점
        for (Map.Entry<Long, Long[]> entry : salesMap.entrySet()) {
            allBranchSales.add(entry);
        }
        
        // 매출이 없는 지점 (0원으로 추가)
        for (Map.Entry<Long, String> branchEntry : allBranchNames.entrySet()) {
            if (!salesMap.containsKey(branchEntry.getKey())) {
                allBranchSales.add(new AbstractMap.SimpleEntry<>(branchEntry.getKey(), new Long[]{0L, 0L}));
            }
        }

        if (allBranchSales.isEmpty()) {
            return TopBranchResponseDto.builder()
                    .branchId(null)
                    .branchName(null)
                    .totalSales(0L)
                    .totalOrders(0L)
                    .month(String.format("%04d-%02d", y, m))
                    .averageSales(0L)
                    .differenceFromAverage(0L)
                    .build();
        }

        // 매출이 가장 낮은 지점 찾기 (오름차순 정렬)
        allBranchSales.sort((a, b) -> Long.compare(a.getValue()[0], b.getValue()[0]));
        Map.Entry<Long, Long[]> low = allBranchSales.get(0);
        Long branchId = low.getKey();
        Long totalSales = low.getValue()[0];
        Long totalOrders = low.getValue()[1];

        // 평균 매출 계산
        Long totalAllSales = allBranchSales.stream()
                .mapToLong(entry -> entry.getValue()[0])
                .sum();
        Long averageSales = allBranchSales.isEmpty() ? 0L : totalAllSales / allBranchSales.size();
        Long differenceFromAverage = totalSales - averageSales;

        // 브랜치네임이 없으면 다시 조회 시도
        String branchName = allBranchNames.getOrDefault(branchId, null);
        if (branchName == null) {
            log.warn("브랜치네임이 없어서 재조회 시도: branchId={}", branchId);
            branchName = getBranchName(branchId);
            if (branchName == null || branchName.equals("Branch-" + branchId)) {
                log.error("브랜치네임 조회 실패: branchId={}", branchId);
            }
        }

        return TopBranchResponseDto.builder()
                .branchId(branchId)
                .branchName(branchName != null ? branchName : "Branch-" + branchId)
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .month(String.format("%04d-%02d", y, m))
                .averageSales(averageSales)
                .differenceFromAverage(differenceFromAverage)
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
     * Branch 서비스에서 Branch 이름 조회 (단일)
     */
    private String getBranchName(Long branchId) {
        try {
            List<Long> branchIds = Collections.singletonList(branchId);
            Map<String, Object> response = branchClient.getBranchesByIds(branchIds);
            log.debug("getBranchName 응답: branchId={}, response={}", branchId, response);

            Object resultObj = response.get("result");
            List<Map<String, Object>> branchesData = new ArrayList<>();
            
            if (resultObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                branchesData = list;
            } else if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                Object dataObj = resultMap.get("data");
                if (dataObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                    branchesData = list;
                }
            }

            if (!branchesData.isEmpty()) {
                Map<String, Object> branch = branchesData.get(0);
                Object nameObj = branch.get("name");
                if (nameObj != null) {
                    String branchName = String.valueOf(nameObj);
                    log.debug("지점 이름 조회 성공: branchId={}, branchName={}", branchId, branchName);
                    return branchName;
                }
            }
        } catch (Exception e) {
            log.error("Branch 이름 조회 실패: branchId={}, error={}", branchId, e.getMessage(), e);
        }
        return "Branch-" + branchId;
    }

    /**
     * Branch 서비스에서 Branch 이름 조회 (여러 개)
     */
    private Map<Long, String> getBranchNamesMap(List<Long> branchIds) {
        try {
            Map<String, Object> response = branchClient.getBranchesByIds(branchIds);
            log.debug("getBranchNamesMap 응답: branchIds={}, response={}", branchIds, response);

            Object resultObj = response.get("result");
            List<Map<String, Object>> branchesData = new ArrayList<>();
            
            if (resultObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                branchesData = list;
            } else if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                Object dataObj = resultMap.get("data");
                if (dataObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj;
                    branchesData = list;
                }
            }

            Map<Long, String> result = new HashMap<>();
            for (Map<String, Object> branch : branchesData) {
                try {
                    Object idObj = branch.get("id");
                    Object nameObj = branch.get("name");
                    if (idObj != null && nameObj != null) {
                        Long branchId = ((Number) idObj).longValue();
                        String branchName = String.valueOf(nameObj);
                        result.put(branchId, branchName);
                        log.debug("지점 정보 추가: branchId={}, branchName={}", branchId, branchName);
                    }
                } catch (Exception e) {
                    log.warn("지점 정보 파싱 실패: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Branch 이름 조회 실패: branchIds={}, error={}", branchIds, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 카테고리별 매출 비중 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @param periodType 기간 타입 (WEEK, MONTH, YEAR)
     */
    public CategorySalesResponseDto getCategorySales(LocalDate startDate, LocalDate endDate, String periodType) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 카테고리별 매출 통계 조회
        List<Object[]> results = orderedItemRepository.findCategorySalesStatistics(startDateTime, endDateTime);

        // 전체 매출 계산
        Long totalSales = results.stream()
                .mapToLong(result -> ((Number) result[2]).longValue())
                .sum();

        // 카테고리별 DTO 생성 및 비율 계산
        List<CategorySalesDto> categories = results.stream()
                .map(result -> {
                    Long categoryId = ((Number) result[0]).longValue();
                    String categoryName = (String) result[1];
                    Long sales = ((Number) result[2]).longValue();
                    Long quantity = ((Number) result[3]).longValue();
                    Long orderCount = ((Number) result[4]).longValue();

                    Double percentage = totalSales > 0 ? (sales.doubleValue() / totalSales) * 100 : 0.0;

                    return CategorySalesDto.builder()
                            .categoryId(categoryId)
                            .categoryName(categoryName)
                            .totalSales(sales)
                            .totalQuantity(quantity)
                            .orderCount(orderCount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        return CategorySalesResponseDto.builder()
                .periodType(periodType != null ? periodType : "MONTH")
                .totalSales(totalSales)
                .categories(categories)
                .build();
    }

    /**
     * 지점별 매출 집계 조회 (지점간 비교용)
     * @param startDate 시작일
     * @param endDate 종료일
     * @param periodType 기간 타입 (WEEK, MONTH, YEAR)
     */
    public BranchSalesSummaryResponseDto getBranchSalesSummary(LocalDate startDate, LocalDate endDate, String periodType) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 지점별 매출 통계 조회
        List<Object[]> results = orderRepository.findBranchSalesStatistics(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 전체 매출 계산
        Long totalSales = results.stream()
                .mapToLong(result -> ((Number) result[1]).longValue())
                .sum();

        // 지점 ID 목록 추출
        List<Long> branchIds = results.stream()
                .map(result -> ((Number) result[0]).longValue())
                .collect(Collectors.toList());

        // 지점 이름 조회
        Map<Long, String> branchNames = getBranchNamesMap(branchIds);

        // 모든 지점 조회 (매출이 0인 지점도 포함)
        List<BranchSalesSummaryDto> allBranches = new ArrayList<>();
        try {
            Map<String, Object> allBranchResponse = branchClient.getBranchesByIds(Collections.emptyList());
            List<Map<String, Object>> allBranchList = new ArrayList<>();
            
            // 응답이 Map인 경우 data 필드에서 리스트 추출
            if (allBranchResponse != null) {
                Object data = allBranchResponse.get("data");
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> branchList = (List<Map<String, Object>>) data;
                    allBranchList = branchList;
                } else if (allBranchResponse.containsKey("result")) {
                    Object result = allBranchResponse.get("result");
                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        Object resultData = resultMap.get("data");
                        if (resultData instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> branchList = (List<Map<String, Object>>) resultData;
                            allBranchList = branchList;
                        }
                    }
                }
            }
            
            Set<Long> branchesWithSales = new HashSet<>(branchIds);
            
            // 매출이 있는 지점들
            for (Object[] result : results) {
                Long branchId = ((Number) result[0]).longValue();
                Long sales = ((Number) result[1]).longValue();
                Long orders = ((Number) result[2]).longValue();
                Long avgOrderAmount = orders > 0 ? sales / orders : 0L;

                String branchName = branchNames.getOrDefault(branchId, "Branch-" + branchId);
                if (branchName == null || branchName.equals("Branch-" + branchId)) {
                    branchName = getBranchName(branchId);
                }

                allBranches.add(BranchSalesSummaryDto.builder()
                        .branchId(branchId)
                        .branchName(branchName != null ? branchName : "Branch-" + branchId)
                        .totalSales(sales)
                        .totalOrders(orders)
                        .averageOrderAmount(avgOrderAmount)
                        .build());
            }

            // 매출이 0인 지점들 추가
            for (Map<String, Object> branch : allBranchList) {
                Object idObj = branch.get("id");
                if (idObj == null) continue;
                
                Long branchId = ((Number) idObj).longValue();
                if (!branchesWithSales.contains(branchId)) {
                    String branchName = (String) branch.get("name");
                    allBranches.add(BranchSalesSummaryDto.builder()
                            .branchId(branchId)
                            .branchName(branchName != null ? branchName : "Branch-" + branchId)
                            .totalSales(0L)
                            .totalOrders(0L)
                            .averageOrderAmount(0L)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("전체 지점 조회 실패, 매출이 있는 지점만 반환: {}", e.getMessage());
            // 매출이 있는 지점만 반환
            for (Object[] result : results) {
                Long branchId = ((Number) result[0]).longValue();
                Long sales = ((Number) result[1]).longValue();
                Long orders = ((Number) result[2]).longValue();
                Long avgOrderAmount = orders > 0 ? sales / orders : 0L;

                String branchName = branchNames.getOrDefault(branchId, "Branch-" + branchId);
                if (branchName == null || branchName.equals("Branch-" + branchId)) {
                    branchName = getBranchName(branchId);
                }

                allBranches.add(BranchSalesSummaryDto.builder()
                        .branchId(branchId)
                        .branchName(branchName != null ? branchName : "Branch-" + branchId)
                        .totalSales(sales)
                        .totalOrders(orders)
                        .averageOrderAmount(avgOrderAmount)
                        .build());
            }
        }

        // 매출 순으로 정렬
        allBranches.sort(Comparator.comparing(BranchSalesSummaryDto::getTotalSales).reversed());

        // 총 지점 수
        Integer totalBranchCount = allBranches.size();

        return BranchSalesSummaryResponseDto.builder()
                .periodType(periodType != null ? periodType : "MONTH")
                .totalSales(totalSales)
                .totalBranchCount(totalBranchCount)
                .branches(allBranches)
                .build();
    }

    /**
     * 전일 대비 증가율 계산
     * @param startDate 시작일
     * @param endDate 종료일
     * @param currentTotalSales 현재 기간 총 매출
     * @param currentTotalOrders 현재 기간 총 주문 수
     * @param periodType 기간 타입
     * @return 증가율 정보
     */
    private GrowthRateDto calculateGrowthRate(LocalDate startDate, LocalDate endDate,
                                               Long currentTotalSales, Long currentTotalOrders,
                                               String periodType) {
        try {
            // 기간 길이 계산
            long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

            // 이전 기간 계산 (같은 길이의 이전 기간)
            LocalDate prevStartDate = startDate.minusDays(periodDays);
            LocalDate prevEndDate = startDate.minusDays(1);

            LocalDateTime prevStartDateTime = prevStartDate.atStartOfDay();
            LocalDateTime prevEndDateTime = prevEndDate.atTime(LocalTime.MAX);

            // 이전 기간 데이터 조회
            List<Order> prevOrders = orderRepository.findAllByOrderStatusAndCreatedAtBetween(
                    OrderStatus.CONFIRMED, prevStartDateTime, prevEndDateTime);

            Long prevTotalSales = prevOrders.stream().mapToLong(Order::getTotalAmount).sum();
            Long prevTotalOrders = (long) prevOrders.size();

            // 증가율 계산 (전일 대비)
            Double totalSalesGrowth = calculatePercentageGrowth(prevTotalSales, currentTotalSales);
            Double totalOrdersGrowth = calculatePercentageGrowth(prevTotalOrders, currentTotalOrders);

            // 월간 매출 증가율 계산 (이번 달 vs 지난 달)
            Double monthlySalesGrowth = calculateMonthlySalesGrowth(endDate);

            log.info("증가율 계산 완료 - 매출: {}%, 주문: {}%, 월간: {}%",
                    totalSalesGrowth, totalOrdersGrowth, monthlySalesGrowth);

            return GrowthRateDto.builder()
                    .totalSalesGrowth(totalSalesGrowth)
                    .monthlySalesGrowth(monthlySalesGrowth)
                    .totalOrdersGrowth(totalOrdersGrowth)
                    .build();

        } catch (Exception e) {
            log.error("증가율 계산 중 오류 발생: {}", e.getMessage());
            // 오류 발생 시 0% 반환
            return GrowthRateDto.builder()
                    .totalSalesGrowth(0.0)
                    .monthlySalesGrowth(0.0)
                    .totalOrdersGrowth(0.0)
                    .build();
        }
    }

    /**
     * 증가율 계산 헬퍼 메서드
     * @param previousValue 이전 값
     * @param currentValue 현재 값
     * @return 증가율 (%)
     */
    private Double calculatePercentageGrowth(Long previousValue, Long currentValue) {
        if (previousValue == null || previousValue == 0) {
            // 이전 값이 0이면 현재 값이 있을 때만 100% 증가로 표시
            return currentValue > 0 ? 100.0 : 0.0;
        }

        double growth = ((currentValue.doubleValue() - previousValue.doubleValue()) / previousValue.doubleValue()) * 100;
        // 소수점 둘째 자리까지 반올림
        return Math.round(growth * 100.0) / 100.0;
    }

    /**
     * 월간 매출 증가율 계산 (이번 달 vs 지난 달)
     * @param endDate 기준 날짜
     * @return 월간 증가율 (%)
     */
    private Double calculateMonthlySalesGrowth(LocalDate endDate) {
        try {
            // 이번 달 첫날부터 endDate까지
            LocalDate currentMonthStart = endDate.withDayOfMonth(1);
            LocalDateTime currentMonthStartDateTime = currentMonthStart.atStartOfDay();
            LocalDateTime currentMonthEndDateTime = endDate.atTime(LocalTime.MAX);

            // 이번 달 매출 조회
            List<Order> currentMonthOrders = orderRepository.findAllByOrderStatusAndCreatedAtBetween(
                    OrderStatus.CONFIRMED, currentMonthStartDateTime, currentMonthEndDateTime);
            Long currentMonthSales = currentMonthOrders.stream().mapToLong(Order::getTotalAmount).sum();

            // 지난 달 같은 기간 (1일부터 같은 일자까지)
            LocalDate prevMonthStart = currentMonthStart.minusMonths(1);
            LocalDate prevMonthEnd = prevMonthStart.plusDays(
                    java.time.temporal.ChronoUnit.DAYS.between(currentMonthStart, endDate));

            // 만약 지난 달이 해당 일자가 없으면 (예: 3월 31일 -> 2월 28일) 마지막 날로 조정
            if (prevMonthEnd.getMonthValue() != prevMonthStart.getMonthValue()) {
                prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());
            }

            LocalDateTime prevMonthStartDateTime = prevMonthStart.atStartOfDay();
            LocalDateTime prevMonthEndDateTime = prevMonthEnd.atTime(LocalTime.MAX);

            // 지난 달 매출 조회
            List<Order> prevMonthOrders = orderRepository.findAllByOrderStatusAndCreatedAtBetween(
                    OrderStatus.CONFIRMED, prevMonthStartDateTime, prevMonthEndDateTime);
            Long prevMonthSales = prevMonthOrders.stream().mapToLong(Order::getTotalAmount).sum();

            return calculatePercentageGrowth(prevMonthSales, currentMonthSales);

        } catch (Exception e) {
            log.error("월간 증가율 계산 중 오류 발생: {}", e.getMessage());
            return 0.0;
        }
    }
}
