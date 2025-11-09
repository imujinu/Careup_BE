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
        log.info("[대시보드 조회 시작] branchId={}, period={}", branchId, period);
        // 오늘 날짜 고정: 2025-11-09
        LocalDate today = LocalDate.of(2025, 11, 9);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        log.info("[날짜 범위] today={}, startOfMonth={}, endOfMonth={}", today, startOfMonth, endOfMonth);

        try {
            log.info("[1단계] 매출 현황 조회 시작");
            SalesSummaryCardDto salesSummary = getSalesSummary(branchId, startOfMonth, endOfMonth);
            log.info("[1단계 완료] 매출 현황: totalSales={}, monthlySales={}, totalOrders={}",
                    salesSummary.getTotalSales(), salesSummary.getMonthlySales(), salesSummary.getTotalOrders());

            log.info("[2단계] 재고 현황 조회 시작");
            InventorySummaryCardDto inventorySummary = getInventorySummary(branchId);
            log.info("[2단계 완료] 재고 현황: totalProducts={}, lowStockProducts={}, stockFulfillmentRate={}",
                    inventorySummary.getTotalProducts(), inventorySummary.getLowStockProducts(), inventorySummary.getStockFulfillmentRate());

            log.info("[3단계] 직원 현황 조회 시작");
            EmployeeSummaryCardDto employeeSummary = getEmployeeSummary(branchId, today);
            log.info("[3단계 완료] 직원 현황: totalEmployees={}, presentEmployees={}, absentEmployees={}, attendanceRate={}",
                    employeeSummary.getTotalEmployees(), employeeSummary.getPresentEmployees(),
                    employeeSummary.getAbsentEmployees(), employeeSummary.getTodayAttendanceRate());

            log.info("[4단계] 주문 현황 조회 시작");
            OrderSummaryCardDto orderSummary = getOrderSummary(branchId, startOfMonth, endOfMonth);
            log.info("[4단계 완료] 주문 현황: totalOrders={}, completedOrders={}, pendingOrders={}, canceledOrders={}",
                    orderSummary.getTotalOrders(), orderSummary.getCompletedOrders(),
                    orderSummary.getPendingOrders(), orderSummary.getCanceledOrders());

            log.info("[5단계] 매출 추이 조회 시작");
            SalesTrendCardDto salesTrend = getSalesTrend(branchId, period);
            log.info("[5단계 완료] 매출 추이: period={}, totalSales={}, salesData size={}",
                    salesTrend.getPeriod(), salesTrend.getTotalSales(),
                    salesTrend.getSalesData() != null ? salesTrend.getSalesData().size() : 0);

            log.info("[6단계] 카테고리별 매출 조회 시작");
            CategorySalesCardDto categorySales = getCategorySales(branchId, startOfMonth, endOfMonth);
            log.info("[6단계 완료] 카테고리별 매출: totalSales={}, topCategory={}, topCategorySales={}",
                    categorySales.getTotalSales(), categorySales.getTopCategory(), categorySales.getTopCategorySales());

            log.info("[7단계] 출근 현황 조회 시작");
            AttendanceSummaryCardDto attendanceSummary = getAttendanceSummary(branchId, today, period);
            log.info("[7단계 완료] 출근 현황: averageAttendanceRate={}, totalWorkDays={}, lateCount={}",
                    attendanceSummary.getAverageAttendanceRate(), attendanceSummary.getTotalWorkDays(), attendanceSummary.getLateCount());

            log.info("[대시보드 조회 완료] branchId={}", branchId);
            return DashboardResponseDto.builder()
                    .salesSummary(salesSummary)
                    .inventorySummary(inventorySummary)
                    .employeeSummary(employeeSummary)
                    .orderSummary(orderSummary)
                    .salesTrend(salesTrend)
                    .categorySales(categorySales)
                    .attendanceSummary(attendanceSummary)
                    .build();
        } catch (Exception e) {
            log.error("[대시보드 조회 실패] branchId={}, 오류 메시지: {}", branchId, e.getMessage(), e);
            throw new RuntimeException("대시보드 데이터를 가져오는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 매출 현황 조회
     */
    private SalesSummaryCardDto getSalesSummary(Long branchId, LocalDate startDate, LocalDate endDate) {
        log.debug("[매출 현황 조회] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);
        try {
            OrderingDashboardClient.SalesSummaryDto dto =
                    orderingDashboardClient.getSalesSummary(branchId, startDate, endDate);
            log.debug("[매출 현황 API 응답] dto={}", dto);

            List<SalesSummaryCardDto.DailySalesDto> last7Days = Optional.ofNullable(dto.last7DaysSales)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(d -> SalesSummaryCardDto.DailySalesDto.builder()
                            .date(d.date)
                            .sales(d.sales)
                            .build())
                    .collect(Collectors.toList());

            log.debug("[매출 현황 변환] last7Days size={}", last7Days.size());
            SalesSummaryCardDto result = SalesSummaryCardDto.builder()
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .monthlySales(dto.monthlySales != null ? dto.monthlySales : 0L)
                    .totalOrders(dto.totalOrders != null ? dto.totalOrders : 0L)
                    .last7DaysSales(last7Days)
                    .build();
            log.debug("[매출 현황 결과] totalSales={}, monthlySales={}, totalOrders={}",
                    result.getTotalSales(), result.getMonthlySales(), result.getTotalOrders());
            return result;
        } catch (Exception e) {
            log.error("[매출 현황 조회 실패] branchId={}, 오류: {}", branchId, e.getMessage(), e);
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
        log.debug("[재고 현황 조회] branchId={}", branchId);
        try {
            OrderingDashboardClient.InventorySummaryDto dto =
                    orderingDashboardClient.getInventorySummary(branchId);
            log.debug("[재고 현황 API 응답] dto={}", dto);

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

            log.debug("[재고 현황 변환] alerts size={}", alerts.size());
            InventorySummaryCardDto result = InventorySummaryCardDto.builder()
                    .totalProducts(dto.totalProducts != null ? dto.totalProducts : 0L)
                    .lowStockProducts(dto.lowStockProducts != null ? dto.lowStockProducts : 0L)
                    .stockFulfillmentRate(dto.stockFulfillmentRate != null ? dto.stockFulfillmentRate : 0.0)
                    .stockAlerts(alerts)
                    .build();
            log.debug("[재고 현황 결과] totalProducts={}, lowStockProducts={}, stockFulfillmentRate={}",
                    result.getTotalProducts(), result.getLowStockProducts(), result.getStockFulfillmentRate());
            return result;
        } catch (Exception e) {
            log.error("[재고 현황 조회 실패] branchId={}, 오류: {}", branchId, e.getMessage(), e);
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
        log.info("[직원 현황 조회 시작] branchId={}, today={}", branchId, today);
        try {
            // 현재 지점에 배치된 직원 수 조회
            List<com.careup.branch.domain.employee.entity.DispatchStatus> dispatches =
                    dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, today);
            long totalEmployees = dispatches.size();
            log.info("[직원 현황] 배치된 총 직원 수={}, 직원 목록: {}", totalEmployees,
                    dispatches.stream().map(d -> d.getEmployee().getName()).collect(Collectors.toList()));

            // 오늘의 스케줄 조회
            List<Schedule> todaySchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), today, today);
            log.info("[직원 현황] 오늘({}) 스케줄 수={}", today, todaySchedules.size());

            if (todaySchedules.isEmpty()) {
                log.warn("[직원 현황] 오늘 날짜의 스케줄이 없습니다. branchId={}, today={}", branchId, today);
            } else {
                log.debug("[직원 현황] 스케줄 직원 목록: {}",
                        todaySchedules.stream().map(s -> s.getEmployee().getName()).collect(Collectors.toList()));
            }

            // 출근 중인 직원 수 계산 (CLOCKED_IN, ON_BREAK, OVERTIME 상태)
            long presentEmployees = todaySchedules.stream()
                    .filter(s -> {
                        ScheduleEvent lastEvent = getLastEvent(s);
                        if (lastEvent == null) {
                            log.debug("[직원 현황] 스케줄 ID {} (직원: {}) - 이벤트 없음", s.getId(), s.getEmployee().getName());
                            return false;
                        }
                        AttendanceStatus status = lastEvent.getAttendanceStatus();
                        boolean isPresent = status == AttendanceStatus.CLOCKED_IN ||
                               status == AttendanceStatus.ON_BREAK ||
                               status == AttendanceStatus.OVERTIME;
                        log.debug("[직원 현황] 스케줄 ID {} (직원: {}) - 상태: {}, 출근 여부: {}",
                                s.getId(), s.getEmployee().getName(), status, isPresent);
                        return isPresent;
                    })
                    .count();
            log.info("[직원 현황] 출근 중인 직원 수={}", presentEmployees);

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
            log.debug("[직원 현황] 결근/휴가 직원 수={}", absentEmployees);

            // 출근률 계산
            double attendanceRate = totalEmployees > 0
                    ? (double) presentEmployees / totalEmployees * 100
                    : 0.0;
            log.debug("[직원 현황] 출근률={}%", attendanceRate);

            EmployeeSummaryCardDto result = EmployeeSummaryCardDto.builder()
                    .totalEmployees(totalEmployees)
                    .presentEmployees(presentEmployees)
                    .absentEmployees(absentEmployees)
                    .todayAttendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .build();
            log.debug("[직원 현황 결과] totalEmployees={}, presentEmployees={}, absentEmployees={}, attendanceRate={}",
                    result.getTotalEmployees(), result.getPresentEmployees(), result.getAbsentEmployees(), result.getTodayAttendanceRate());
            return result;
        } catch (Exception e) {
            log.error("[직원 현황 조회 실패] branchId={}, 오류: {}", branchId, e.getMessage(), e);
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
        log.debug("[주문 현황 조회] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);
        try {
            OrderingDashboardClient.OrderSummaryDto dto =
                    orderingDashboardClient.getOrderSummary(branchId, startDate, endDate);
            log.debug("[주문 현황 API 응답] dto={}", dto);

            OrderSummaryCardDto result = OrderSummaryCardDto.builder()
                    .totalOrders(dto.totalOrders != null ? dto.totalOrders : 0L)
                    .completedOrders(dto.completedOrders != null ? dto.completedOrders : 0L)
                    .pendingOrders(dto.pendingOrders != null ? dto.pendingOrders : 0L)
                    .canceledOrders(dto.canceledOrders != null ? dto.canceledOrders : 0L)
                    .orderStatusDistribution(dto.orderStatusDistribution != null ? dto.orderStatusDistribution : Collections.emptyMap())
                    .build();
            log.debug("[주문 현황 결과] totalOrders={}, completedOrders={}, pendingOrders={}, canceledOrders={}",
                    result.getTotalOrders(), result.getCompletedOrders(), result.getPendingOrders(), result.getCanceledOrders());
            return result;
        } catch (Exception e) {
            log.error("[주문 현황 조회 실패] branchId={}, 오류: {}", branchId, e.getMessage(), e);
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
        log.debug("[매출 추이 조회] branchId={}, period={}", branchId, period);
        try {
            Integer year = LocalDate.now().getYear();
            log.debug("[매출 추이] year={}", year);
            OrderingDashboardClient.SalesTrendDto dto =
                    orderingDashboardClient.getSalesTrend(branchId, period, year);
            log.debug("[매출 추이 API 응답] dto={}", dto);

            List<SalesTrendCardDto.PeriodSalesDto> salesData = Optional.ofNullable(dto.salesData)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(d -> SalesTrendCardDto.PeriodSalesDto.builder()
                            .periodLabel(d.periodLabel)
                            .sales(d.sales)
                            .build())
                    .collect(Collectors.toList());

            log.debug("[매출 추이 변환] salesData size={}", salesData.size());
            SalesTrendCardDto result = SalesTrendCardDto.builder()
                    .period(dto.period)
                    .salesData(salesData)
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .yearOverYearGrowth(dto.yearOverYearGrowth != null ? dto.yearOverYearGrowth : 0.0)
                    .goalAchievementRate(dto.goalAchievementRate != null ? dto.goalAchievementRate : 0.0)
                    .build();
            log.debug("[매출 추이 결과] period={}, totalSales={}, yearOverYearGrowth={}, goalAchievementRate={}",
                    result.getPeriod(), result.getTotalSales(), result.getYearOverYearGrowth(), result.getGoalAchievementRate());
            return result;
        } catch (Exception e) {
            log.error("[매출 추이 조회 실패] branchId={}, period={}, 오류: {}", branchId, period, e.getMessage(), e);
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
        log.debug("[카테고리별 매출 조회] branchId={}, startDate={}, endDate={}", branchId, startDate, endDate);
        try {
            OrderingDashboardClient.CategorySalesDto dto =
                    orderingDashboardClient.getCategorySales(branchId, startDate, endDate);
            log.debug("[카테고리별 매출 API 응답] dto={}", dto);

            CategorySalesCardDto result = CategorySalesCardDto.builder()
                    .categorySalesDistribution(dto.categorySalesDistribution != null ? dto.categorySalesDistribution : Collections.emptyMap())
                    .totalSales(dto.totalSales != null ? dto.totalSales : 0L)
                    .topCategory(dto.topCategory)
                    .topCategorySales(dto.topCategorySales != null ? dto.topCategorySales : 0L)
                    .build();
            log.debug("[카테고리별 매출 결과] totalSales={}, topCategory={}, topCategorySales={}, categories count={}",
                    result.getTotalSales(), result.getTopCategory(), result.getTopCategorySales(),
                    result.getCategorySalesDistribution() != null ? result.getCategorySalesDistribution().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("[카테고리별 매출 조회 실패] branchId={}, 오류: {}", branchId, e.getMessage(), e);
            return CategorySalesCardDto.builder()
                    .categorySalesDistribution(Collections.emptyMap())
                    .totalSales(0L)
                    .topCategory(null)
                    .topCategorySales(0L)
                    .build();
        }
    }

    /**
     * 출근 현황 조회 (주간/월간/연간)
     *
     * @param branchId 지점 ID
     * @param today 기준 날짜
     * @param period 조회 기간 (WEEKLY, MONTHLY, YEARLY)
     */
    private AttendanceSummaryCardDto getAttendanceSummary(Long branchId, LocalDate today, String period) {
        log.debug("[출근 현황 조회] branchId={}, today={}, period={}", branchId, today, period);
        try {
            // 기간에 따른 시작/종료 날짜 계산
            LocalDate startDate;
            LocalDate endDate = today;

            switch (period.toUpperCase()) {
                case "WEEKLY":
                    // 최근 7일
                    startDate = today.minusDays(6);
                    break;
                case "MONTHLY":
                    // 이번 달 1일부터 오늘까지
                    startDate = today.withDayOfMonth(1);
                    break;
                case "YEARLY":
                    // 올해 1월 1일부터 오늘까지
                    startDate = today.withDayOfYear(1);
                    break;
                default:
                    log.warn("[출근 현황] 알 수 없는 기간: {}. 기본값(WEEKLY) 사용", period);
                    startDate = today.minusDays(6);
                    period = "WEEKLY";
            }

            log.debug("[출근 현황] startDate={}, endDate={}", startDate, endDate);

            // 현재 지점에 배치된 총 직원 수
            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, today).size();
            log.debug("[출근 현황] 전체 직원 수={}", totalEmployees);

            // 기간별 스케줄 조회
            List<Schedule> periodSchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), startDate, endDate);
            log.debug("[출근 현황] 기간별 스케줄 수={}", periodSchedules.size());

            // 날짜별로 그룹화
            Map<LocalDate, List<Schedule>> schedulesByDate = periodSchedules.stream()
                    .collect(Collectors.groupingBy(Schedule::getRegisteredDate));

            // 기간별 출근 현황 데이터 생성
            Map<LocalDate, AttendanceSummaryCardDto.AttendanceDayDto> attendanceData = new LinkedHashMap<>();
            long totalWorkDays = 0;
            long lateCount = 0;
            double totalRate = 0.0;

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
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

                log.debug("[출근 현황 일별] date={}, present={}, absent={}, late={}, rate={}%",
                        date, presentCount, absentCount, dayLateCount, attendanceRate);

                attendanceData.put(date, AttendanceSummaryCardDto.AttendanceDayDto.builder()
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

            log.debug("[출근 현황 결과] period={}, averageAttendanceRate={}, totalWorkDays={}, lateCount={}",
                    period, averageAttendanceRate, totalWorkDays, lateCount);

            return AttendanceSummaryCardDto.builder()
                    .period(period)
                    .attendanceData(attendanceData)
                    .averageAttendanceRate(Math.round(averageAttendanceRate * 100.0) / 100.0)
                    .totalWorkDays(totalWorkDays)
                    .lateCount(lateCount)
                    .build();
        } catch (Exception e) {
            log.error("[출근 현황 조회 실패] branchId={}, period={}, 오류: {}", branchId, period, e.getMessage(), e);
            return AttendanceSummaryCardDto.builder()
                    .period(period)
                    .attendanceData(Collections.emptyMap())
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

