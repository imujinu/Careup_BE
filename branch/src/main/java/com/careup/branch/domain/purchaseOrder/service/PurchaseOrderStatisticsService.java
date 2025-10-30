package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.purchaseOrder.dto.HQStatisticsResponseDto;
import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderDetailRepository;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 발주 통계 서비스
 * - 본사용 통계 조회 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PurchaseOrderStatisticsService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final OrderingInventoryClient orderingInventoryClient;
    private final BranchRepository branchRepository;

    /**
     * 본사용 전체 통계 조회 (통합)
     */
    public HQStatisticsResponseDto getHQStatistics(LocalDate startDate, LocalDate endDate) {
        // 본사 관리자 권한 검증
        validateHQAdminAccess();

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("본사 통계 조회: {} ~ {}", start, end);

        // 기간별 발주 목록 조회
        List<PurchaseOrder> orders = purchaseOrderRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        // 1. 전체 발주 현황 계산
        HQStatisticsResponseDto.OverallStatistics overallStats = calculateOverallStatistics(orders);

        // 2. 상태별 통계 계산
        List<HQStatisticsResponseDto.StatusStatistics> statusStats = calculateStatusStatistics(orders);

        // 3. 지점별 통계 계산 (TOP 10)
        List<HQStatisticsResponseDto.BranchStatistics> branchStats = calculateBranchStatistics(startDateTime, endDateTime);

        // 4. 상품별 통계 계산 (TOP 10)
        List<HQStatisticsResponseDto.ProductStatistics> productStats = calculateProductStatistics(startDateTime, endDateTime);

        return HQStatisticsResponseDto.builder()
                .overallStatistics(overallStats)
                .statusStatistics(statusStats)
                .branchStatistics(branchStats)
                .productStatistics(productStats)
                .build();
    }

    /**
     * 본사용 전체현황 통계 조회
     */
    public HQStatisticsResponseDto.OverallStatistics getHQOverallStatistics(LocalDate startDate, LocalDate endDate) {
        // 본사 관리자 권한 검증
        validateHQAdminAccess();

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("본사 전체현황 통계 조회: {} ~ {}", start, end);

        // 기간별 발주 목록 조회
        List<PurchaseOrder> orders = purchaseOrderRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        return calculateOverallStatistics(orders);
    }

    /**
     * 본사용 상태별 통계 조회
     */
    public List<HQStatisticsResponseDto.StatusStatistics> getHQStatusStatistics(LocalDate startDate, LocalDate endDate) {
        // 본사 관리자 권한 검증
        validateHQAdminAccess();

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("본사 상태별 통계 조회: {} ~ {}", start, end);

        // 기간별 발주 목록 조회
        List<PurchaseOrder> orders = purchaseOrderRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        return calculateStatusStatistics(orders);
    }

    /**
     * 본사용 지점별 통계 조회
     */
    public List<HQStatisticsResponseDto.BranchStatistics> getHQBranchStatistics(LocalDate startDate, LocalDate endDate) {
        // 본사 관리자 권한 검증
        validateHQAdminAccess();

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("본사 지점별 통계 조회: {} ~ {}", start, end);

        return calculateBranchStatistics(startDateTime, endDateTime);
    }

    /**
     * 본사용 상품별 통계 조회
     */
    public List<HQStatisticsResponseDto.ProductStatistics> getHQProductStatistics(LocalDate startDate, LocalDate endDate) {
        // 본사 관리자 권한 검증
        validateHQAdminAccess();

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("본사 상품별 통계 조회: {} ~ {}", start, end);

        return calculateProductStatistics(startDateTime, endDateTime);
    }

    // === 내부 계산 메서드들 ===

    /**
     * 전체 발주 현황 계산
     */
    private HQStatisticsResponseDto.OverallStatistics calculateOverallStatistics(List<PurchaseOrder> orders) {
        if (orders.isEmpty()) {
            return HQStatisticsResponseDto.OverallStatistics.builder()
                    .totalOrderCount(0L)
                    .totalOrderAmount(0L)
                    .totalApprovedAmount(0L)
                    .averageOrderAmount(0.0)
                    .pendingCount(0L)
                    .approvalRate(0.0)
                    .rejectionRate(0.0)
                    .partialApprovalRate(0.0)
                    .build();
        }

        int totalCount = orders.size();
        long totalAmount = orders.stream().mapToLong(PurchaseOrder::getPrice).sum();
        double avgAmount = (double) totalAmount / totalCount;

        // 상태별 카운트
        Map<OrderStatus, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getOrderStatus, Collectors.counting()));

        long pendingCount = statusCounts.getOrDefault(OrderStatus.PENDING, 0L);
        long approvedCount = statusCounts.getOrDefault(OrderStatus.APPROVED, 0L);
        long partialCount = statusCounts.getOrDefault(OrderStatus.PARTIAL, 0L);
        long rejectedCount = statusCounts.getOrDefault(OrderStatus.REJECTED, 0L);

        // 승인된 발주의 총 금액 (APPROVED, PARTIAL, SHIPPED, COMPLETED)
        long approvedAmount = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.APPROVED ||
                        o.getOrderStatus() == OrderStatus.PARTIAL ||
                        o.getOrderStatus() == OrderStatus.SHIPPED ||
                        o.getOrderStatus() == OrderStatus.COMPLETED)
                .mapToLong(PurchaseOrder::getPrice)
                .sum();

        // 비율 계산
        double approvalRate = ((approvedCount + partialCount) * 100.0) / totalCount;
        double rejectionRate = (rejectedCount * 100.0) / totalCount;
        double partialApprovalRate = (partialCount * 100.0) / totalCount;

        return HQStatisticsResponseDto.OverallStatistics.builder()
                .totalOrderCount((long) totalCount)
                .totalOrderAmount(totalAmount)
                .totalApprovedAmount(approvedAmount)
                .averageOrderAmount(Math.round(avgAmount * 100.0) / 100.0)
                .pendingCount(pendingCount)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .rejectionRate(Math.round(rejectionRate * 100.0) / 100.0)
                .partialApprovalRate(Math.round(partialApprovalRate * 100.0) / 100.0)
                .build();
    }

    /**
     * 상태별 통계 계산
     */
    private List<HQStatisticsResponseDto.StatusStatistics> calculateStatusStatistics(List<PurchaseOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        int totalCount = orders.size();

        // 상태별로 그룹핑
        Map<OrderStatus, List<PurchaseOrder>> groupedByStatus = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getOrderStatus));

        // EnumMap을 사용하여 모든 상태에 대한 통계 생성
        Map<OrderStatus, HQStatisticsResponseDto.StatusStatistics> statsMap = new EnumMap<>(OrderStatus.class);

        for (OrderStatus status : OrderStatus.values()) {
            List<PurchaseOrder> statusOrders = groupedByStatus.getOrDefault(status, List.of());
            int count = statusOrders.size();
            long totalAmount = statusOrders.stream().mapToLong(PurchaseOrder::getPrice).sum();
            double percentage = (count * 100.0) / totalCount;

            statsMap.put(status, HQStatisticsResponseDto.StatusStatistics.builder()
                    .status(status)
                    .count((long) count)
                    .totalAmount(totalAmount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        // 건수가 있는 상태만 반환 (건수 내림차순)
        return statsMap.values().stream()
                .filter(stat -> stat.getCount() > 0)
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    /**
     * 지점별 통계 계산
     */
    private List<HQStatisticsResponseDto.BranchStatistics> calculateBranchStatistics(
            LocalDateTime startDate, LocalDateTime endDate) {

        List<Object[]> results = purchaseOrderRepository.findBranchStatistics(startDate, endDate);

        return results.stream()
                .limit(10) // TOP 10만
                .map(result -> {
                    Long branchId = (Long) result[0];
                    Long orderCount = (Long) result[1];
                    Long totalAmount = result[2] != null ? ((Number) result[2]).longValue() : 0L;
                    Double avgAmount = result[3] != null ? ((Number) result[3]).doubleValue() : 0.0;
                    Long approvedCount = (Long) result[4];
                    Long rejectedCount = (Long) result[5];

                    // 지점명 조회
                    String branchName = "지점-" + branchId;
                    try {
                        var branch = branchRepository.findById(branchId);
                        if (branch.isPresent() && branch.get().getName() != null) {
                            branchName = branch.get().getName();
                        }
                    } catch (Exception e) {
                        log.warn("지점 정보 조회 실패: branchId={}", branchId, e);
                    }

                    double approvalRate = orderCount > 0 ? (approvedCount * 100.0) / orderCount : 0.0;

                    return HQStatisticsResponseDto.BranchStatistics.builder()
                            .branchId(branchId)
                            .branchName(branchName)
                            .orderCount(orderCount)
                            .totalAmount(totalAmount)
                            .averageAmount(Math.round(avgAmount * 100.0) / 100.0)
                            .approvedCount(approvedCount)
                            .rejectedCount(rejectedCount)
                            .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 상품별 통계 계산
     */
    private List<HQStatisticsResponseDto.ProductStatistics> calculateProductStatistics(
            LocalDateTime startDate, LocalDateTime endDate) {

        List<Object[]> results = purchaseOrderDetailRepository.findProductStatistics(startDate, endDate);

        return results.stream()
                .limit(10) // TOP 10만
                .map(result -> {
                    Long productId = (Long) result[0];
                    String productName = (String) result[1]; // 저장된 상품명 사용
                    Long totalQuantity = result[2] != null ? ((Number) result[2]).longValue() : 0L;
                    Long approvedQuantity = result[3] != null ? ((Number) result[3]).longValue() : 0L;
                    Long totalAmount = result[4] != null ? ((Number) result[4]).longValue() : 0L;
                    Long orderCount = (Long) result[5];

                    // 상품명이 null이거나 비어있으면 기본값 사용
                    if (productName == null || productName.trim().isEmpty()) {
                        productName = "상품-" + productId;
                    }

                    double approvalRate = totalQuantity > 0 ? (approvedQuantity * 100.0) / totalQuantity : 0.0;

                    return HQStatisticsResponseDto.ProductStatistics.builder()
                            .productId(productId)
                            .productName(productName)
                            .totalQuantity(totalQuantity)
                            .approvedQuantity(approvedQuantity)
                            .totalAmount(totalAmount)
                            .orderCount(orderCount)
                            .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 본사 관리자 권한 검증
     */
    private void validateHQAdminAccess() {
        try {
            // 1. 현재 인증된 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new SecurityException("인증되지 않은 사용자입니다.");
            }

            // 2. JWT Claims에서 사용자 정보 추출
            Claims claims = (Claims) auth.getDetails();
            if (claims == null) {
                throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            }

            String role = claims.get("role", String.class);

            // 3. 본사 관리자 권한 확인
            if (!"HQ_ADMIN".equals(role)) {
                log.warn("본사 통계 접근 권한 없음 - role: {}", role);
                throw new SecurityException("본사 관리자만 접근할 수 있습니다.");
            }

            log.info("본사 관리자 권한 확인 완료 - role: {}", role);

        } catch (Exception e) {
            throw new SecurityException("본사 관리자 권한 검증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 가맹점용 발주 통계 조회
     * @param branchId 가맹점 ID
     */
    public HQStatisticsResponseDto.FranchiseStatistics getFranchiseStatistics(Long branchId, LocalDate startDate, LocalDate endDate) {
        validateBranchAccess(branchId);

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        log.info("가맹점 {} 통계 조회: {} ~ {}", branchId, start, end);

        // 특정 가맹점의 발주 조회
        List<PurchaseOrder> orders = purchaseOrderRepository.findByBranchIdAndCreatedAtBetween(branchId, startDateTime, endDateTime);

        // 전체 통계 계산
        long totalOrders = orders.size();
        long pendingOrders = orders.stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING).count();
        long approvedOrders = orders.stream().filter(o -> o.getOrderStatus() == OrderStatus.APPROVED || o.getOrderStatus() == OrderStatus.SHIPPED || o.getOrderStatus() == OrderStatus.COMPLETED).count();
        long totalAmount = orders.stream().mapToLong(PurchaseOrder::getPrice).sum();
        long approvedAmount = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.APPROVED || o.getOrderStatus() == OrderStatus.SHIPPED || o.getOrderStatus() == OrderStatus.COMPLETED)
                .mapToLong(PurchaseOrder::getPrice)
                .sum();

        return HQStatisticsResponseDto.FranchiseStatistics.builder()
                .branchId(branchId)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .approvedOrders(approvedOrders)
                .totalAmount(totalAmount)
                .approvedAmount(approvedAmount)
                .approvalRate(totalOrders > 0 ? Math.round((approvedOrders * 100.0) / totalOrders * 100.0) / 100.0 : 0.0)
                .build();
    }

    /**
     * 가맹점용 상품별 발주 통계 조회
     */
    public List<HQStatisticsResponseDto.ProductStatistics> getFranchiseProductStatistics(Long branchId, LocalDate startDate, LocalDate endDate) {
        // 권한 검증: 본인 가맹점만 조회 가능
        validateBranchAccess(branchId);

        // 기간 설정 (기본값: 최근 1개월)
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        // 특정 가맹점의 발주 상세 조회
        List<PurchaseOrder> orders = purchaseOrderRepository.findByBranchIdAndCreatedAtBetween(branchId, startDateTime, endDateTime);

        // 상품별 통계 계산
        class TempProductStats {
            Long productId;
            String productName;
            Long totalQuantity = 0L;
            Long approvedQuantity = 0L;
            Long totalAmount = 0L;
            Long orderCount = 0L;
        }

        Map<Long, TempProductStats> productStatsMap = new HashMap<>();

        for (PurchaseOrder order : orders) {
            for (com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail detail : order.getOrderDetails()) {
                Long productId = detail.getProductId();

                TempProductStats stats = productStatsMap.computeIfAbsent(productId, k -> {
                    TempProductStats s = new TempProductStats();
                    s.productId = productId;

                    // 저장된 상품명 사용
                    String productName = detail.getProductName();
                    if (productName == null || productName.trim().isEmpty()) {
                        productName = "상품-" + productId;
                    }
                    s.productName = productName;

                    return s;
                });

                stats.totalQuantity += detail.getQuantity();
                stats.totalAmount += detail.getSubtotalPrice();

                if (order.getOrderStatus() == OrderStatus.APPROVED ||
                        order.getOrderStatus() == OrderStatus.SHIPPED ||
                        order.getOrderStatus() == OrderStatus.COMPLETED) {
                    stats.approvedQuantity += detail.getApprovedQuantity();
                }

                stats.orderCount += 1;
            }
        }

        // 승인율 계산 및 정렬
        List<HQStatisticsResponseDto.ProductStatistics> result = productStatsMap.values().stream()
                .map(stats -> {
                    double approvalRate = stats.totalQuantity > 0
                            ? (stats.approvedQuantity * 100.0) / stats.totalQuantity
                            : 0.0;

                    return HQStatisticsResponseDto.ProductStatistics.builder()
                            .productId(stats.productId)
                            .productName(stats.productName)
                            .totalQuantity(stats.totalQuantity)
                            .approvedQuantity(stats.approvedQuantity)
                            .totalAmount(stats.totalAmount)
                            .orderCount(stats.orderCount)
                            .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getTotalQuantity(), a.getTotalQuantity())) // 발주량 많은 순으로 정렬
                .limit(10) // TOP 10만
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 가맹점 권한 검증
     */
    private void validateBranchAccess(Long requestedBranchId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new SecurityException("인증되지 않은 사용자입니다.");
            }

            Claims claims = (Claims) auth.getDetails();
            if (claims == null) {
                throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            }

            Long userBranchId = claims.get("branchId", Long.class);

            String role = claims.get("role", String.class);
            if ("HQ_ADMIN".equals(role)) {
                return;
            }

            if (userBranchId == null || !userBranchId.equals(requestedBranchId)) {
                throw new SecurityException("본인 가맹점만 접근할 수 있습니다.");
            }

        } catch (Exception e) {
            throw new SecurityException("가맹점 권한 검증에 실패했습니다: " + e.getMessage());
        }
    }
}
