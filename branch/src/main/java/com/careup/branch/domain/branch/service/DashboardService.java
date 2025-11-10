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
     * @param period 조회 기간 (WEEKLY: 최근 7일, MONTHLY: 최근 7개월, YEARLY: 최근 7년)
     */
    private AttendanceSummaryCardDto getAttendanceSummary(Long branchId, LocalDate today, String period) {
        log.debug("[출근 현황 조회] branchId={}, today={}, period={}", branchId, today, period);
        try {
            String normalizedPeriod = period.toUpperCase();

            switch (normalizedPeriod) {
                case "WEEKLY":
                    return getWeeklyAttendance(branchId, today);
                case "MONTHLY":
                    return getMonthlyAttendance(branchId, today);
                case "YEARLY":
                    return getYearlyAttendance(branchId, today);
                default:
                    log.warn("[출근 현황] 알 수 없는 기간: {}. 기본값(WEEKLY) 사용", period);
                    return getWeeklyAttendance(branchId, today);
            }
        } catch (Exception e) {
            log.error("[출근 현황 조회 실패] branchId={}, period={}, 오류: {}", branchId, period, e.getMessage(), e);
            return AttendanceSummaryCardDto.builder()
                    .period(period)
                    .chartData(Collections.emptyList())
                    .averageAttendanceRate(0.0)
                    .totalWorkDays(0L)
                    .lateCount(0L)
                    .build();
        }
    }

    /**
     * 주간 출근 현황 (최근 7일)
     */
    private AttendanceSummaryCardDto getWeeklyAttendance(Long branchId, LocalDate today) {
        log.debug("[주간 출근 현황] branchId={}, today={}", branchId, today);

        List<AttendanceSummaryCardDto.ChartDataDto> chartData = new ArrayList<>();

        long totalWorkDays = 0;
        long lateCount = 0;
        double totalRate = 0.0;

        // 최근 7일간 일별 데이터 생성
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // 해당 날짜의 스케줄 조회
            List<Schedule> daySchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), date, date);

            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, date).size();

            long presentCount = countPresentEmployees(daySchedules);
            long absentCount = countAbsentEmployees(daySchedules);
            long dayLateCount = countLateEmployees(daySchedules);

            lateCount += dayLateCount;

            double attendanceRate = totalEmployees > 0
                    ? (double) presentCount / totalEmployees * 100
                    : 0.0;

            // 라벨: "MM/dd" 형식
            String label = String.format("%02d/%02d", date.getMonthValue(), date.getDayOfMonth());

            chartData.add(AttendanceSummaryCardDto.ChartDataDto.builder()
                    .label(label)
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
                ? Math.round((totalRate / totalWorkDays) * 100.0) / 100.0
                : 0.0;

        log.debug("[주간 출근 현황 완료] 평균 출근률={}%, 근무일={}, 지각={}",
                averageAttendanceRate, totalWorkDays, lateCount);

        return AttendanceSummaryCardDto.builder()
                .period("WEEKLY")
                .chartData(chartData)
                .averageAttendanceRate(averageAttendanceRate)
                .totalWorkDays(totalWorkDays)
                .lateCount(lateCount)
                .build();
    }

    /**
     * 월간 출근 현황 (최근 7개월)
     */
    private AttendanceSummaryCardDto getMonthlyAttendance(Long branchId, LocalDate today) {
        log.debug("[월간 출근 현황] branchId={}, today={}", branchId, today);

        List<AttendanceSummaryCardDto.ChartDataDto> chartData = new ArrayList<>();

        long totalWorkDays = 0;
        long lateCount = 0;
        double totalRate = 0.0;

        // 최근 7개월간 월별 데이터 생성
        for (int i = 6; i >= 0; i--) {
            LocalDate targetMonth = today.minusMonths(i);
            LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
            LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());

            // 해당 월의 마지막 날짜가 오늘보다 미래면 오늘까지만
            if (endOfMonth.isAfter(today)) {
                endOfMonth = today;
            }

            // 해당 월의 스케줄 조회
            List<Schedule> monthSchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), startOfMonth, endOfMonth);

            // 해당 월의 일수
            long daysInPeriod = startOfMonth.datesUntil(endOfMonth.plusDays(1)).count();

            // 월별 평균 직원 수 계산 (간단하게 중간 날짜 기준)
            LocalDate midMonth = startOfMonth.plusDays(daysInPeriod / 2);
            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, midMonth).size();

            long presentCount = countPresentEmployees(monthSchedules);
            long absentCount = countAbsentEmployees(monthSchedules);
            long monthLateCount = countLateEmployees(monthSchedules);

            lateCount += monthLateCount;

            double attendanceRate = totalEmployees > 0 && daysInPeriod > 0
                    ? (double) presentCount / (totalEmployees * daysInPeriod) * 100
                    : 0.0;

            // 라벨: "YYYY-MM" 형식
            String label = String.format("%d-%02d", targetMonth.getYear(), targetMonth.getMonthValue());

            chartData.add(AttendanceSummaryCardDto.ChartDataDto.builder()
                    .label(label)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .totalCount(totalEmployees * daysInPeriod)
                    .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .build());

            if (presentCount > 0 || absentCount > 0) {
                totalWorkDays++;
                totalRate += attendanceRate;
            }
        }

        double averageAttendanceRate = totalWorkDays > 0
                ? Math.round((totalRate / totalWorkDays) * 100.0) / 100.0
                : 0.0;

        log.debug("[월간 출근 현황 완료] 평균 출근률={}%, 조회 월 수={}, 지각={}",
                averageAttendanceRate, totalWorkDays, lateCount);

        return AttendanceSummaryCardDto.builder()
                .period("MONTHLY")
                .chartData(chartData)
                .averageAttendanceRate(averageAttendanceRate)
                .totalWorkDays(totalWorkDays)
                .lateCount(lateCount)
                .build();
    }

    /**
     * 연간 출근 현황 (최근 7년)
     */
    private AttendanceSummaryCardDto getYearlyAttendance(Long branchId, LocalDate today) {
        log.debug("[연간 출근 현황] branchId={}, today={}", branchId, today);

        List<AttendanceSummaryCardDto.ChartDataDto> chartData = new ArrayList<>();

        long totalWorkYears = 0;
        long lateCount = 0;
        double totalRate = 0.0;

        // 최근 7년간 연별 데이터 생성
        for (int i = 6; i >= 0; i--) {
            int targetYear = today.getYear() - i;
            LocalDate startOfYear = LocalDate.of(targetYear, 1, 1);
            LocalDate endOfYear = LocalDate.of(targetYear, 12, 31);

            // 해당 년도의 마지막 날짜가 오늘보다 미래면 오늘까지만
            if (endOfYear.isAfter(today)) {
                endOfYear = today;
            }

            // 해당 년도의 스케줄 조회
            List<Schedule> yearSchedules = scheduleRepository.findByBranch_IdInAndRegisteredDateBetween(
                    Collections.singletonList(branchId), startOfYear, endOfYear);

            // 해당 년도의 일수
            long daysInYear = startOfYear.datesUntil(endOfYear.plusDays(1)).count();

            // 연도 중간 날짜 기준 평균 직원 수
            LocalDate midYear = startOfYear.plusDays(daysInYear / 2);
            long totalEmployees = dispatchStatusRepository.findActiveDispatchesByBranchId(branchId, midYear).size();

            long presentCount = countPresentEmployees(yearSchedules);
            long absentCount = countAbsentEmployees(yearSchedules);
            long yearLateCount = countLateEmployees(yearSchedules);

            lateCount += yearLateCount;

            double attendanceRate = totalEmployees > 0 && daysInYear > 0
                    ? (double) presentCount / (totalEmployees * daysInYear) * 100
                    : 0.0;

            // 라벨: "YYYY" 형식
            String label = String.valueOf(targetYear);

            chartData.add(AttendanceSummaryCardDto.ChartDataDto.builder()
                    .label(label)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .totalCount(totalEmployees * daysInYear)
                    .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .build());

            if (presentCount > 0 || absentCount > 0) {
                totalWorkYears++;
                totalRate += attendanceRate;
            }
        }

        double averageAttendanceRate = totalWorkYears > 0
                ? Math.round((totalRate / totalWorkYears) * 100.0) / 100.0
                : 0.0;

        log.debug("[연간 출근 현황 완료] 평균 출근률={}%, 조회 년 수={}, 지각={}",
                averageAttendanceRate, totalWorkYears, lateCount);

        return AttendanceSummaryCardDto.builder()
                .period("YEARLY")
                .chartData(chartData)
                .averageAttendanceRate(averageAttendanceRate)
                .totalWorkDays(totalWorkYears)
                .lateCount(lateCount)
                .build();
    }

    /**
     * 출근 인원 카운트
     */
    private long countPresentEmployees(List<Schedule> schedules) {
        return schedules.stream()
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
    }

    /**
     * 결근 인원 카운트
     */
    private long countAbsentEmployees(List<Schedule> schedules) {
        return schedules.stream()
                .filter(s -> {
                    ScheduleEvent lastEvent = getLastEvent(s);
                    if (lastEvent == null) return false;
                    AttendanceStatus status = lastEvent.getAttendanceStatus();
                    return status == AttendanceStatus.LEAVE ||
                           status == AttendanceStatus.ABSENT;
                })
                .count();
    }

    /**
     * 지각 인원 카운트
     */
    private long countLateEmployees(List<Schedule> schedules) {
        return schedules.stream()
                .filter(s -> {
                    ScheduleEvent lastEvent = getLastEvent(s);
                    if (lastEvent == null) return false;
                    return lastEvent.getAttendanceStatus() == AttendanceStatus.LATE;
                })
                .count();
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

