package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.dashboard.DashboardResponseDto;
import com.careup.branch.domain.branch.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 대시보드 컨트롤러
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 특정 지점의 대시보드 전체 데이터 조회
     *
     * @param branchId 지점 ID
     * @param period 매출 추이 기간 (YEARLY, MONTHLY, WEEKLY) - 기본값: MONTHLY
     * @return 대시보드 전체 데이터
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_MANAGER', 'BRANCH_OWNER')")
    public ResponseEntity<CommonSuccessDto> getDashboard(
            @PathVariable Long branchId,
            @RequestParam(value = "period", defaultValue = "MONTHLY") String period) {

        log.info("=== 대시보드 조회 요청 시작 === branchId={}, period={}", branchId, period);

        try {
            DashboardResponseDto dashboard = dashboardService.getDashboard(branchId, period);

            log.info("=== 대시보드 조회 성공 ===");
            log.info("매출 현황: totalSales={}, monthlySales={}, totalOrders={}",
                    dashboard.getSalesSummary() != null ? dashboard.getSalesSummary().getTotalSales() : "null",
                    dashboard.getSalesSummary() != null ? dashboard.getSalesSummary().getMonthlySales() : "null",
                    dashboard.getSalesSummary() != null ? dashboard.getSalesSummary().getTotalOrders() : "null");
            log.info("재고 현황: totalProducts={}, lowStockProducts={}",
                    dashboard.getInventorySummary() != null ? dashboard.getInventorySummary().getTotalProducts() : "null",
                    dashboard.getInventorySummary() != null ? dashboard.getInventorySummary().getLowStockProducts() : "null");
            log.info("직원 현황: totalEmployees={}, presentEmployees={}",
                    dashboard.getEmployeeSummary() != null ? dashboard.getEmployeeSummary().getTotalEmployees() : "null",
                    dashboard.getEmployeeSummary() != null ? dashboard.getEmployeeSummary().getPresentEmployees() : "null");
            log.info("주문 현황: totalOrders={}, completedOrders={}",
                    dashboard.getOrderSummary() != null ? dashboard.getOrderSummary().getTotalOrders() : "null",
                    dashboard.getOrderSummary() != null ? dashboard.getOrderSummary().getCompletedOrders() : "null");

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(dashboard)
                    .status_code(HttpStatus.OK.value())
                    .status_message("대시보드 데이터를 성공적으로 조회했습니다.")
                    .build();

            log.info("=== 대시보드 조회 완료 === branchId={}", branchId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== 대시보드 조회 실패 === branchId={}, period={}, 오류: {}", branchId, period, e.getMessage(), e);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(null)
                    .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status_message("대시보드 데이터 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 매출 현황만 조회 (개별 카드 조회)
     */
    @GetMapping("/branch/{branchId}/sales-summary")
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_MANAGER', 'BRANCH_OWNER')")
    public ResponseEntity<CommonSuccessDto> getSalesSummary(@PathVariable Long branchId) {
        log.info("매출 현황 조회 요청: branchId={}", branchId);

        try {
            DashboardResponseDto dashboard = dashboardService.getDashboard(branchId, "MONTHLY");

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(dashboard.getSalesSummary())
                    .status_code(HttpStatus.OK.value())
                    .status_message("매출 현황을 성공적으로 조회했습니다.")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("매출 현황 조회 실패: branchId={}", branchId, e);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(null)
                    .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status_message("매출 현황 조회 중 오류가 발생했습니다.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 재고 현황만 조회 (개별 카드 조회)
     */
    @GetMapping("/branch/{branchId}/inventory-summary")
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_MANAGER', 'BRANCH_OWNER')")
    public ResponseEntity<CommonSuccessDto> getInventorySummary(@PathVariable Long branchId) {
        log.info("재고 현황 조회 요청: branchId={}", branchId);

        try {
            DashboardResponseDto dashboard = dashboardService.getDashboard(branchId, "MONTHLY");

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(dashboard.getInventorySummary())
                    .status_code(HttpStatus.OK.value())
                    .status_message("재고 현황을 성공적으로 조회했습니다.")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재고 현황 조회 실패: branchId={}", branchId, e);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(null)
                    .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status_message("재고 현황 조회 중 오류가 발생했습니다.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 직원 현황만 조회 (개별 카드 조회)
     */
    @GetMapping("/branch/{branchId}/employee-summary")
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_MANAGER', 'BRANCH_OWNER')")
    public ResponseEntity<CommonSuccessDto> getEmployeeSummary(@PathVariable Long branchId) {
        log.info("직원 현황 조회 요청: branchId={}", branchId);

        try {
            DashboardResponseDto dashboard = dashboardService.getDashboard(branchId, "MONTHLY");

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(dashboard.getEmployeeSummary())
                    .status_code(HttpStatus.OK.value())
                    .status_message("직원 현황을 성공적으로 조회했습니다.")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("직원 현황 조회 실패: branchId={}", branchId, e);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(null)
                    .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status_message("직원 현황 조회 중 오류가 발생했습니다.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 출근 현황만 조회 (개별 카드 조회)
     *
     * @param branchId 지점 ID
     * @param period 조회 기간 (WEEKLY, MONTHLY, YEARLY) - 기본값: WEEKLY
     * @return 출근 현황 데이터
     */
    @GetMapping("/branch/{branchId}/attendance-summary")
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_MANAGER', 'BRANCH_OWNER')")
    public ResponseEntity<CommonSuccessDto> getAttendanceSummary(
            @PathVariable Long branchId,
            @RequestParam(value = "period", defaultValue = "WEEKLY") String period) {
        log.info("출근 현황 조회 요청: branchId={}, period={}", branchId, period);

        try {
            DashboardResponseDto dashboard = dashboardService.getDashboard(branchId, period);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(dashboard.getAttendanceSummary())
                    .status_code(HttpStatus.OK.value())
                    .status_message("출근 현황을 성공적으로 조회했습니다.")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("출근 현황 조회 실패: branchId={}, period={}", branchId, period, e);

            CommonSuccessDto response = CommonSuccessDto.builder()
                    .result(null)
                    .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .status_message("출근 현황 조회 중 오류가 발생했습니다.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

