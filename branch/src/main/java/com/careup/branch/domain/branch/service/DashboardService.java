package com.careup.branch.domain.branch.service;

import com.careup.branch.common.client.OrderingDashboardClient;
import com.careup.branch.domain.branch.dto.dashboard.*;
import com.careup.branch.domain.employee.entity.AttendanceStatus;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 대시보드 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderingDashboardClient orderingDashboardClient;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    /**
     * 특정 지점의 대시보드 전체 데이터 조회
     */
    public DashboardResponseDto getDashboard(Long branchId, String period) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        try {
            return DashboardResponseDto.builder()
                    .salesSummary(getSalesSummary(branchId, startOfMonth, endOfMonth))
                    .inventorySummary(getInventorySummary(branchId))
                    .employeeSummary(getEmployeeSummary(branchId, today))
                    .orderSummary(getOrderSummary(branchId, startOfMonth, endOfMonth))
                    .salesTrend(getSalesTrend(branchId, period))
                    .categorySales(getCategorySales(branchId, startOfMonth, endOfMonth))
                    .attendanceSummary(getAttendanceSummary(branchId, today))
                    .build();
        } catch (Exception e) {
            log.error("대시보드 조회 중 오류 발생: branchId={}", branchId, e);
            throw new RuntimeException("대시보드 데이터를 가져오는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 매출 현황 조회
     */
    private SalesSummaryCardDto getSalesSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        try {
            OrderingDashboardClient.SalesSummaryDto dto =
                    orderingDashboardClient.getSalesSummary(branchId, startDate, endDate);

            List<SalesSummaryCardDto.DailySalesDto> last7Days = Optional.ofNullable(dto.last7DaysSales)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(d -> SalesSummaryCardDto.DailySalesDto.builder()
                            .date(d.date)
                            .sales(d.sales)
                            .build())
                    .collect(Collectors.toList());

            return SalesSummaryCardDto.builder()
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .monthlySales(dto.monthlySales != null ? dto.monthlySales : 0L)
                    .totalOrders(dto.totalOrders != null ? dto.totalOrders : 0L)
                    .last7DaysSales(last7Days)
                    .build();
        } catch (Exception e) {
            log.error("매출 현황 조회 실패: branchId={}", branchId, e);
            return SalesSummaryCardDto.builder()
                    .totalSales(0L)
                    .monthlySales(0L)
                    .totalOrders(0L)
                    .last7DaysSales(Collections.emptyList())
                    .build();
        }
    }

    /**
     * 재고 현황 조회
     */
    private InventorySummaryCardDto getInventorySummary(Long branchId) {
        try {
            OrderingDashboardClient.InventorySummaryDto dto =
                    orderingDashboardClient.getInventorySummary(branchId);

            List<InventorySummaryCardDto.StockAlertDto> alerts = Optional.ofNullable(dto.stockAlerts)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(a -> InventorySummaryCardDto.StockAlertDto.builder()
                            .productId(a.productId)
                            .productName(a.productName)
                            .currentStock(a.currentStock)
                            .safetyStock(a.safetyStock)
                            .alertLevel(a.alertLevel)
                            .build())
                    .collect(Collectors.toList());

            return InventorySummaryCardDto.builder()
                    .totalProducts(dto.totalProducts != null ? dto.totalProducts : 0L)
                    .lowStockProducts(dto.lowStockProducts != null ? dto.lowStockProducts : 0L)
                    .stockFulfillmentRate(dto.stockFulfillmentRate != null ? dto.stockFulfillmentRate : 0.0)
                    .stockAlerts(alerts)
                    .build();
        } catch (Exception e) {
            log.error("재고 현황 조회 실패: branchId={}", branchId, e);
            return InventorySummaryCardDto.builder()
                    .totalProducts(0L)
                    .lowStockProducts(0L)
                    .stockFulfillmentRate(0.0)
                    .stockAlerts(Collections.emptyList())
                    .build();
        }
    }

    /**
     * 직원 현황 조회
     */
    private EmployeeSummaryCardDto getEmployeeSummary(Long branchId, LocalDate today) {
        try {
            // 현재 지점에 배치된 직원 수 조회
            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, today).size();

            // 오늘의 스케줄 조회
            List<Schedule> todaySchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), today, today);

            // 출근 중인 직원 수 계산 (CLOCKED_IN, ON_BREAK, OVERTIME 상태)
            long presentEmployees = todaySchedules.stream()
                    .filter(s -> {
                        ScheduleEvent lastEvent = getLastEvent(s);
                        if (lastEvent == null) return false;
                        AttendanceStatus status = lastEvent.getAttendanceStatus();
                        return status == AttendanceStatus.CLOCKED_IN ||
                               status == AttendanceStatus.ON_BREAK ||
                               status == AttendanceStatus.OVERTIME;
                    })
                    .count();

            // 결근/휴가 직원 수
            long absentEmployees = todaySchedules.stream()
                    .filter(s -> {
                        ScheduleEvent lastEvent = getLastEvent(s);
                        if (lastEvent == null) return false;
                        AttendanceStatus status = lastEvent.getAttendanceStatus();
                        return status == AttendanceStatus.LEAVE ||
                               status == AttendanceStatus.ABSENT;
                    })
                    .count();

            // 출근률 계산
            double attendanceRate = totalEmployees > 0
                    ? (double) presentEmployees / totalEmployees * 100
                    : 0.0;

            return EmployeeSummaryCardDto.builder()
                    .totalEmployees(totalEmployees)
                    .presentEmployees(presentEmployees)
                    .absentEmployees(absentEmployees)
                    .todayAttendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .build();
        } catch (Exception e) {
            log.error("직원 현황 조회 실패: branchId={}", branchId, e);
            return EmployeeSummaryCardDto.builder()
                    .totalEmployees(0L)
                    .presentEmployees(0L)
                    .absentEmployees(0L)
                    .todayAttendanceRate(0.0)
                    .build();
        }
    }

    /**
     * 주문 현황 조회
     */
    private OrderSummaryCardDto getOrderSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        try {
            OrderingDashboardClient.OrderSummaryDto dto =
                    orderingDashboardClient.getOrderSummary(branchId, startDate, endDate);

            return OrderSummaryCardDto.builder()
                    .totalOrders(dto.totalOrders != null ? dto.totalOrders : 0L)
                    .completedOrders(dto.completedOrders != null ? dto.completedOrders : 0L)
                    .pendingOrders(dto.pendingOrders != null ? dto.pendingOrders : 0L)
                    .canceledOrders(dto.canceledOrders != null ? dto.canceledOrders : 0L)
                    .orderStatusDistribution(dto.orderStatusDistribution != null ? dto.orderStatusDistribution : Collections.emptyMap())
                    .build();
        } catch (Exception e) {
            log.error("주문 현황 조회 실패: branchId={}", branchId, e);
            return OrderSummaryCardDto.builder()
                    .totalOrders(0L)
                    .completedOrders(0L)
                    .pendingOrders(0L)
                    .canceledOrders(0L)
                    .orderStatusDistribution(Collections.emptyMap())
                    .build();
        }
    }

    /**
     * 매출 추이 조회
     */
    private SalesTrendCardDto getSalesTrend(Long branchId, String period) {
        try {
            Integer year = LocalDate.now().getYear();
            OrderingDashboardClient.SalesTrendDto dto =
                    orderingDashboardClient.getSalesTrend(branchId, period, year);

            List<SalesTrendCardDto.PeriodSalesDto> salesData = Optional.ofNullable(dto.salesData)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(d -> SalesTrendCardDto.PeriodSalesDto.builder()
                            .periodLabel(d.periodLabel)
                            .sales(d.sales)
                            .build())
                    .collect(Collectors.toList());

            return SalesTrendCardDto.builder()
                    .period(dto.period)
                    .salesData(salesData)
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .yearOverYearGrowth(dto.yearOverYearGrowth != null ? dto.yearOverYearGrowth : 0.0)
                    .goalAchievementRate(dto.goalAchievementRate != null ? dto.goalAchievementRate : 0.0)
                    .build();
        } catch (Exception e) {
            log.error("매출 추이 조회 실패: branchId={}, period={}", branchId, period, e);
            return SalesTrendCardDto.builder()
                    .period(period)
                    .salesData(Collections.emptyList())
                    .totalSales(0L)
                    .yearOverYearGrowth(0.0)
                    .goalAchievementRate(0.0)
                    .build();
        }
    }

    /**
     * 카테고리별 매출 분석 조회
     */
    private CategorySalesCardDto getCategorySales(Long branchId, LocalDate startDate, LocalDate endDate) {
        try {
            OrderingDashboardClient.CategorySalesDto dto =
                    orderingDashboardClient.getCategorySales(branchId, startDate, endDate);

            return CategorySalesCardDto.builder()
                    .categorySalesDistribution(dto.categorySalesDistribution != null ? dto.categorySalesDistribution : Collections.emptyMap())
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .topCategory(dto.topCategory)
                    .topCategorySales(dto.topCategorySales != null ? dto.topCategorySales : 0L)
                    .build();
        } catch (Exception e) {
            log.error("카테고리별 매출 조회 실패: branchId={}", branchId, e);
            return CategorySalesCardDto.builder()
                    .categorySalesDistribution(Collections.emptyMap())
                    .totalSales(0L)
                    .topCategory(null)
                    .topCategorySales(0L)
                    .build();
        }
    }

    /**
     * 출근 현황 조회 (주간)
     */
    private AttendanceSummaryCardDto getAttendanceSummary(Long branchId, LocalDate today) {
        try {
            // 최근 7일 범위 계산
            LocalDate startDate = today.minusDays(6);

            // 현재 지점에 배치된 총 직원 수
            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, today).size();

            // 7일간의 스케줄 조회
            List<Schedule> weekSchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), startDate, today);

            // 날짜별로 그룹화
            Map<LocalDate, List<Schedule>> schedulesByDate = weekSchedules.stream()
                    .collect(Collectors.groupingBy(Schedule::getRegisteredDate));

            // 주간 출근 현황 데이터 생성
            Map<LocalDate, AttendanceSummaryCardDto.AttendanceDayDto> weeklyAttendance = new LinkedHashMap<>();
            long totalWorkDays = 0;
            long lateCount = 0;
            double totalRate = 0.0;

            for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
                List<Schedule> daySchedules = schedulesByDate.getOrDefault(date, Collections.emptyList());

                long presentCount = daySchedules.stream()
                        .filter(s -> {
                            ScheduleEvent lastEvent = getLastEvent(s);
                            if (lastEvent == null) return false;
                            AttendanceStatus status = lastEvent.getAttendanceStatus();
                            return status == AttendanceStatus.CLOCKED_IN ||
                                   status == AttendanceStatus.ON_BREAK ||
                                   status == AttendanceStatus.OVERTIME ||
                                   status == AttendanceStatus.CLOCKED_OUT ||
                                   status == AttendanceStatus.LATE;
                        })
                        .count();

                long absentCount = daySchedules.stream()
                        .filter(s -> {
                            ScheduleEvent lastEvent = getLastEvent(s);
                            if (lastEvent == null) return false;
                            AttendanceStatus status = lastEvent.getAttendanceStatus();
                            return status == AttendanceStatus.LEAVE ||
                                   status == AttendanceStatus.ABSENT;
                        })
                        .count();

                // 지각 횟수 계산
                long dayLateCount = daySchedules.stream()
                        .filter(s -> {
                            ScheduleEvent lastEvent = getLastEvent(s);
                            if (lastEvent == null) return false;
                            return lastEvent.getAttendanceStatus() == AttendanceStatus.LATE;
                        })
                        .count();

                lateCount += dayLateCount;

                double attendanceRate = totalEmployees > 0
                        ? (double) presentCount / totalEmployees * 100
                        : 0.0;

                weeklyAttendance.put(date, AttendanceSummaryCardDto.AttendanceDayDto.builder()
                        .presentCount(presentCount)
                        .absentCount(absentCount)
                        .totalCount(totalEmployees)
                        .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                        .build());

                if (presentCount > 0 || absentCount > 0) {
                    totalWorkDays++;
                    totalRate += attendanceRate;
                }
            }

            double averageAttendanceRate = totalWorkDays > 0
                    ? totalRate / totalWorkDays
                    : 0.0;

            return AttendanceSummaryCardDto.builder()
                    .weeklyAttendance(weeklyAttendance)
                    .averageAttendanceRate(Math.round(averageAttendanceRate * 100.0) / 100.0)
                    .totalWorkDays(totalWorkDays)
                    .lateCount(lateCount)
                    .build();
        } catch (Exception e) {
            log.error("출근 현황 조회 실패: branchId={}", branchId, e);
            return AttendanceSummaryCardDto.builder()
                    .weeklyAttendance(Collections.emptyMap())
                    .averageAttendanceRate(0.0)
                    .totalWorkDays(0L)
                    .lateCount(0L)
                    .build();
        }
    }

    /**
     * 스케줄의 마지막 이벤트 가져오기 (상태 판단용)
     */
    private ScheduleEvent getLastEvent(Schedule schedule) {
        try {
            Optional<ScheduleEvent> event = scheduleEventRepository.findByScheduleId(schedule.getId());
            return event.orElse(null);
        } catch (Exception e) {
            log.warn("스케줄 이벤트 조회 실패: scheduleId={}", schedule.getId(), e);
            return null;
        }
    }
}

