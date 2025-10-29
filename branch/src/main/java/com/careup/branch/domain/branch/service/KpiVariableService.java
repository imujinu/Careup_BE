package com.careup.branch.domain.branch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * KPI 계산에 필요한 변수 데이터를 제공하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KpiVariableService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 특정 지점의 KPI 변수 값을 조회
     * @param branchId 지점 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 변수명-값 맵
     */
    public Map<String, Double> getKpiVariables(Long branchId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> variables = new HashMap<>();

        // 매출 관련 변수
        variables.putAll(getSalesVariables(branchId, startDate, endDate));

        // 주문 관련 변수
        variables.putAll(getOrderVariables(branchId, startDate, endDate));

        // 재고 관련 변수
        variables.putAll(getInventoryVariables(branchId, startDate, endDate));

        // 출근 관련 변수
        variables.putAll(getAttendanceVariables(branchId, startDate, endDate));

        log.info("지점 ID: {}, 변수 조회 완료: {}", branchId, variables);
        return variables;
    }

    /**
     * 매출 관련 변수 조회
     */
    private Map<String, Double> getSalesVariables(Long branchId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> variables = new HashMap<>();

        try {
            // 총 매출액 (total_sales)
            String salesQuery = """
                SELECT COALESCE(SUM(total_price), 0) as total_sales
                FROM orders
                WHERE branch_id = ? 
                AND order_date BETWEEN ? AND ?
                AND status = 'COMPLETED'
                """;
            BigDecimal totalSales = jdbcTemplate.queryForObject(
                salesQuery,
                BigDecimal.class,
                branchId, startDate, endDate
            );
            variables.put("total_sales", totalSales != null ? totalSales.doubleValue() : 0.0);

            // 평균 객단가 (avg_order_value)
            String avgOrderQuery = """
                SELECT COALESCE(AVG(total_price), 0) as avg_order
                FROM orders
                WHERE branch_id = ? 
                AND order_date BETWEEN ? AND ?
                AND status = 'COMPLETED'
                """;
            BigDecimal avgOrder = jdbcTemplate.queryForObject(
                avgOrderQuery,
                BigDecimal.class,
                branchId, startDate, endDate
            );
            variables.put("avg_order_value", avgOrder != null ? avgOrder.doubleValue() : 0.0);

        } catch (Exception e) {
            log.warn("매출 변수 조회 실패: {}", e.getMessage());
            variables.put("total_sales", 0.0);
            variables.put("avg_order_value", 0.0);
        }

        return variables;
    }

    /**
     * 주문 관련 변수 조회
     */
    private Map<String, Double> getOrderVariables(Long branchId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> variables = new HashMap<>();

        try {
            // 총 주문 건수 (total_orders)
            String orderCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as total_orders
                FROM orders
                WHERE branch_id = ? 
                AND order_date BETWEEN ? AND ?
                """;
            Long totalOrders = jdbcTemplate.queryForObject(
                orderCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("total_orders", totalOrders != null ? totalOrders.doubleValue() : 0.0);

            // 완료된 주문 건수 (completed_orders)
            String completedOrderQuery = """
                SELECT COALESCE(COUNT(*), 0) as completed_orders
                FROM orders
                WHERE branch_id = ? 
                AND order_date BETWEEN ? AND ?
                AND status = 'COMPLETED'
                """;
            Long completedOrders = jdbcTemplate.queryForObject(
                completedOrderQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("completed_orders", completedOrders != null ? completedOrders.doubleValue() : 0.0);

            // 취소된 주문 건수 (cancelled_orders)
            String cancelledOrderQuery = """
                SELECT COALESCE(COUNT(*), 0) as cancelled_orders
                FROM orders
                WHERE branch_id = ? 
                AND order_date BETWEEN ? AND ?
                AND status = 'CANCELLED'
                """;
            Long cancelledOrders = jdbcTemplate.queryForObject(
                cancelledOrderQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("cancelled_orders", cancelledOrders != null ? cancelledOrders.doubleValue() : 0.0);

        } catch (Exception e) {
            log.warn("주문 변수 조회 실패: {}", e.getMessage());
            variables.put("total_orders", 0.0);
            variables.put("completed_orders", 0.0);
            variables.put("cancelled_orders", 0.0);
        }

        return variables;
    }

    /**
     * 재고 관련 변수 조회
     */
    private Map<String, Double> getInventoryVariables(Long branchId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> variables = new HashMap<>();

        try {
            // 발주 건수 (purchase_order_count)
            String poCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as po_count
                FROM purchase_order
                WHERE branch_id = ? 
                AND DATE(created_at) BETWEEN ? AND ?
                """;
            Long poCount = jdbcTemplate.queryForObject(
                poCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("purchase_order_count", poCount != null ? poCount.doubleValue() : 0.0);

            // 총 발주 금액 (total_purchase_amount)
            String poAmountQuery = """
                SELECT COALESCE(SUM(price), 0) as total_amount
                FROM purchase_order
                WHERE branch_id = ? 
                AND DATE(created_at) BETWEEN ? AND ?
                """;
            BigDecimal totalAmount = jdbcTemplate.queryForObject(
                poAmountQuery,
                BigDecimal.class,
                branchId, startDate, endDate
            );
            variables.put("total_purchase_amount", totalAmount != null ? totalAmount.doubleValue() : 0.0);

            // 승인된 발주 건수 (approved_order_count) - NEW
            String approvedCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as approved_count
                FROM purchase_order
                WHERE branch_id = ? 
                AND DATE(created_at) BETWEEN ? AND ?
                AND order_status IN ('APPROVED', 'COMPLETED')
                """;
            Long approvedCount = jdbcTemplate.queryForObject(
                approvedCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("approved_order_count", approvedCount != null ? approvedCount.doubleValue() : 0.0);

            // 거절된 발주 건수 (rejected_order_count) - NEW
            String rejectedCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as rejected_count
                FROM purchase_order
                WHERE branch_id = ? 
                AND DATE(created_at) BETWEEN ? AND ?
                AND order_status = 'REJECTED'
                """;
            Long rejectedCount = jdbcTemplate.queryForObject(
                rejectedCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("rejected_order_count", rejectedCount != null ? rejectedCount.doubleValue() : 0.0);

            // 평균 발주 금액 (avg_purchase_amount) - NEW
            if (poCount != null && poCount > 0 && totalAmount != null) {
                double avgAmount = totalAmount.doubleValue() / poCount.doubleValue();
                variables.put("avg_purchase_amount", avgAmount);
            } else {
                variables.put("avg_purchase_amount", 0.0);
            }

            // 발주 승인율 (purchase_approval_rate) - NEW
            if (poCount != null && poCount > 0 && approvedCount != null) {
                double approvalRate = (approvedCount.doubleValue() / poCount.doubleValue()) * 100;
                variables.put("purchase_approval_rate", approvalRate);
            } else {
                variables.put("purchase_approval_rate", 0.0);
            }

            // 총 발주 수량 (total_purchase_quantity) - NEW
            String totalQuantityQuery = """
                SELECT COALESCE(SUM(pod.quantity), 0) as total_quantity
                FROM purchase_order_detail pod
                JOIN purchase_order po ON pod.order_id = po.purchase_order_id
                WHERE po.branch_id = ? 
                AND DATE(po.created_at) BETWEEN ? AND ?
                """;
            Long totalQuantity = jdbcTemplate.queryForObject(
                totalQuantityQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("total_purchase_quantity", totalQuantity != null ? totalQuantity.doubleValue() : 0.0);

            // 총 승인 수량 (total_approved_quantity) - NEW
            String approvedQuantityQuery = """
                SELECT COALESCE(SUM(pod.approved_quantity), 0) as approved_quantity
                FROM purchase_order_detail pod
                JOIN purchase_order po ON pod.order_id = po.purchase_order_id
                WHERE po.branch_id = ? 
                AND DATE(po.created_at) BETWEEN ? AND ?
                """;
            Long approvedQuantity = jdbcTemplate.queryForObject(
                approvedQuantityQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("total_approved_quantity", approvedQuantity != null ? approvedQuantity.doubleValue() : 0.0);

            // 수량 승인율 (quantity_approval_rate) - NEW
            if (totalQuantity != null && totalQuantity > 0 && approvedQuantity != null) {
                double quantityApprovalRate = (approvedQuantity.doubleValue() / totalQuantity.doubleValue()) * 100;
                variables.put("quantity_approval_rate", quantityApprovalRate);
            } else {
                variables.put("quantity_approval_rate", 0.0);
            }

            // 평균 재고 회전율 (inventory_turnover) - 기존 유지
            String inventoryValueQuery = """
                SELECT COALESCE(SUM(i.quantity * i.unit_price), 0) as inventory_value
                FROM inventory i
                WHERE i.branch_id = ?
                """;
            BigDecimal inventoryValue = jdbcTemplate.queryForObject(
                inventoryValueQuery,
                BigDecimal.class,
                branchId
            );

            if (inventoryValue != null && inventoryValue.doubleValue() > 0 && totalAmount != null) {
                double turnover = totalAmount.doubleValue() / inventoryValue.doubleValue();
                variables.put("inventory_turnover", turnover);
            } else {
                variables.put("inventory_turnover", 0.0);
            }

        } catch (Exception e) {
            log.warn("재고 변수 조회 실패: {}", e.getMessage());
            variables.put("purchase_order_count", 0.0);
            variables.put("total_purchase_amount", 0.0);
            variables.put("approved_order_count", 0.0);
            variables.put("rejected_order_count", 0.0);
            variables.put("avg_purchase_amount", 0.0);
            variables.put("purchase_approval_rate", 0.0);
            variables.put("total_purchase_quantity", 0.0);
            variables.put("total_approved_quantity", 0.0);
            variables.put("quantity_approval_rate", 0.0);
            variables.put("inventory_turnover", 0.0);
        }

        return variables;
    }

    /**
     * 출근 관련 변수 조회
     */
    private Map<String, Double> getAttendanceVariables(Long branchId, LocalDate startDate, LocalDate endDate) {
        Map<String, Double> variables = new HashMap<>();

        try {
            // 총 근무 일수 (total_work_days)
            String workDaysQuery = """
                SELECT COALESCE(COUNT(DISTINCT DATE(check_in_time)), 0) as work_days
                FROM attendance
                WHERE branch_id = ? 
                AND DATE(check_in_time) BETWEEN ? AND ?
                """;
            Long workDays = jdbcTemplate.queryForObject(
                workDaysQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("total_work_days", workDays != null ? workDays.doubleValue() : 0.0);

            // 총 근무 시간 (total_work_hours)
            String workHoursQuery = """
                SELECT COALESCE(SUM(TIMESTAMPDIFF(HOUR, check_in_time, check_out_time)), 0) as work_hours
                FROM attendance
                WHERE branch_id = ? 
                AND DATE(check_in_time) BETWEEN ? AND ?
                AND check_out_time IS NOT NULL
                """;
            Long workHours = jdbcTemplate.queryForObject(
                workHoursQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("total_work_hours", workHours != null ? workHours.doubleValue() : 0.0);

            // 지각 횟수 (late_count)
            String lateCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as late_count
                FROM attendance
                WHERE branch_id = ? 
                AND DATE(check_in_time) BETWEEN ? AND ?
                AND status = 'LATE'
                """;
            Long lateCount = jdbcTemplate.queryForObject(
                lateCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("late_count", lateCount != null ? lateCount.doubleValue() : 0.0);

            // 결근 횟수 (absent_count)
            String absentCountQuery = """
                SELECT COALESCE(COUNT(*), 0) as absent_count
                FROM attendance
                WHERE branch_id = ? 
                AND DATE(check_in_time) BETWEEN ? AND ?
                AND status = 'ABSENT'
                """;
            Long absentCount = jdbcTemplate.queryForObject(
                absentCountQuery,
                Long.class,
                branchId, startDate, endDate
            );
            variables.put("absent_count", absentCount != null ? absentCount.doubleValue() : 0.0);

        } catch (Exception e) {
            log.warn("출근 변수 조회 실패: {}", e.getMessage());
            variables.put("total_work_days", 0.0);
            variables.put("total_work_hours", 0.0);
            variables.put("late_count", 0.0);
            variables.put("absent_count", 0.0);
        }

        return variables;
    }
}

