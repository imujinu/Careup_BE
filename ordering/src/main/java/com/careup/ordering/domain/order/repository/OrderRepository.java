package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.SalesStatisticsDto;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    @Override
    List<SalesStatisticsDto> findMonthlySalesStatistics(OrderStatus status, LocalDateTime start, LocalDateTime end);

    @Override
    AllBranchesSalesDto calculateTotalSalesStats(LocalDateTime start, LocalDateTime end);

    // 회원별 주문 조회
    List<Order> findByMemberId(Long memberId);

    // 지점별 주문 조회
    List<Order> findByBranchId(Long branchId);

    // 주문 상태별 조회
    List<Order> findByOrderStatus(OrderStatus status);

    // 기간별 주문 조회
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 특정 지점의 기간별 주문 조회
    List<Order> findByBranchIdAndOrderStatusAndCreatedAtBetween(
            Long branchId, OrderStatus orderStatus, LocalDateTime startDate, LocalDateTime endDate);

    // 특정 지점의 총 매출액 조회
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.branchId = :branchId " +
           "AND o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalSalesByBranchAndPeriod(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 특정 지점의 주문 수 조회
    @Query("SELECT COUNT(o) FROM Order o WHERE o.branchId = :branchId " +
           "AND o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countOrdersByBranchAndPeriod(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 인근 지역 가맹점들의 평균 매출액 조회 (branchId 리스트 기반)
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.branchId IN :branchIds " +
           "AND o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Double calculateAverageSalesByBranches(
            @Param("branchIds") List<Long> branchIds,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ========== HQ_ADMIN 전용 쿼리 메서드 ==========

    // 전체 지점의 총 매출액 조회
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalSalesAllBranches(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);



    // 전체 지점의 총 주문 수 조회
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countOrdersAllBranches(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 전체 지점의 기간별 주문 조회
    @Query("SELECT o FROM Order o " +
           "WHERE o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY o.createdAt ASC")
    List<Order> findAllByOrderStatusAndCreatedAtBetween(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 활성 지점 수 조회 (주문이 있는 지점)
    @Query("SELECT COUNT(DISTINCT o.branchId) FROM Order o " +
           "WHERE o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    Integer countActiveBranches(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    // 지점별 매출 정보 조회 (지점 ID, 총 매출, 주문 수)
    @Query("SELECT o.branchId, SUM(o.totalAmount), COUNT(o) FROM Order o " +
           "WHERE o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY o.branchId ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findBranchSalesStatistics(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 특정 지점들의 매출 비교 데이터 조회
    @Query("SELECT o.branchId, SUM(o.totalAmount), COUNT(o) FROM Order o " +
           "WHERE o.branchId IN :branchIds AND o.orderStatus = :status " +
           "AND o.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY o.branchId ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findBranchSalesComparison(
            @Param("branchIds") List<Long> branchIds,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // PENDING 상태이고 특정 시간 이전에 생성된 주문 조회 (타임아웃 주문 찾기용)
    List<Order> findByOrderStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);
}
