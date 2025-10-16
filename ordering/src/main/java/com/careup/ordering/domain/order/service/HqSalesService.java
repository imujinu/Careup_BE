package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.BranchSalesDetailDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.repository.OrderRepository;
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
    private final BranchClient branchClient;

    /**
     * 전체 지점 매출 내역 기간별 조회
     */
    public AllBranchesSalesResponseDto getAllBranchesSales(HqSalesRequestDto request) {
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(LocalTime.MAX);

        // 전체 지점 주문 조회
        List<Order> orders = orderRepository.findAllByOrderStatusAndCreatedAtBetween(
                OrderStatus.CONFIRMED, startDateTime, endDateTime);

        // 기간별 통계 계산
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
     * 일별 전체 지점 매출 통계
     */
    private List<AllBranchesSalesDto> calculateAllBranchesDailySales(List<Order> orders) {
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
     * 월별 전체 지점 매출 통계
     */
    private List<AllBranchesSalesDto> calculateAllBranchesMonthlySales(List<Order> orders) {
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

        // 기간별 상세 데이터
        List<BranchSalesDetailDto> salesData;
        String periodType = request.getPeriodType() != null ? request.getPeriodType() : "DAY";

        switch (periodType.toUpperCase()) {
            case "WEEK":
                salesData = calculateBranchWeeklySales(branchId, branchOrders);
                break;
            case "MONTH":
                salesData = calculateBranchMonthlySales(branchId, branchOrders);
                break;
            default:
                salesData = calculateBranchDailySales(branchId, branchOrders);
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
                .branchName("Branch-" + branchId) // 실제로는 Branch 서비스에서 조회
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
    private List<BranchSalesDetailDto> calculateBranchDailySales(Long branchId, List<Order> orders) {
        Map<LocalDate, List<Order>> dailyOrders = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));

        return dailyOrders.entrySet().stream()
                .map(entry -> {
                    Long totalSales = entry.getValue().stream().mapToLong(Order::getTotalAmount).sum();
                    Long totalOrders = (long) entry.getValue().size();

                    return BranchSalesDetailDto.builder()
                            .branchId(branchId)
                            .branchName("Branch-" + branchId)
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
    private List<BranchSalesDetailDto> calculateBranchWeeklySales(Long branchId, List<Order> orders) {
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
                            .branchName("Branch-" + branchId)
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
    private List<BranchSalesDetailDto> calculateBranchMonthlySales(Long branchId, List<Order> orders) {
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
                            .branchName("Branch-" + branchId)
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

        // 비교 데이터 생성
        List<BranchSalesDetailDto> comparisonData = new ArrayList<>();
        Map<Long, String> branchNames = new HashMap<>();

        int ranking = 1;
        for (Object[] stat : comparisonStats) {
            Long branchId = ((Number) stat[0]).longValue();
            Long sales = ((Number) stat[1]).longValue();
            Long orders = ((Number) stat[2]).longValue();

            String branchName = "Branch-" + branchId; // 실제로는 Branch 서비스에서 조회
            branchNames.put(branchId, branchName);

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

            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) response.get("result");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branchesData = (List<Map<String, Object>>) resultMap;

            if (!branchesData.isEmpty()) {
                return (String) branchesData.get(0).get("name");
            }
        } catch (Exception e) {
            log.error("Branch 이름 조회 실패: {}", e.getMessage());
        }
        return "Branch-" + branchId;
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
}
