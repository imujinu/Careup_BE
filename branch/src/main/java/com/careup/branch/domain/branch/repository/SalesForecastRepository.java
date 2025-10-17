package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.SalesForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesForecastRepository extends JpaRepository<SalesForecast, Long> {

    /**
     * 특정 지점의 예상 매출 목록 조회
     */
    List<SalesForecast> findByBranchIdOrderByPeriodStartDesc(Long branchId);

    /**
     * 특정 지점의 특정 기간 예상 매출 조회
     */
    @Query("SELECT sf FROM SalesForecast sf WHERE sf.branch.id = :branchId " +
           "AND sf.periodStart <= :targetDate AND sf.period_end >= :targetDate")
    Optional<SalesForecast> findByBranchIdAndPeriod(
            @Param("branchId") Long branchId,
            @Param("targetDate") Date targetDate);

    /**
     * 특정 기간의 모든 지점 예상 매출 조회
     */
    @Query("SELECT sf FROM SalesForecast sf WHERE sf.periodStart <= :targetDate " +
           "AND sf.period_end >= :targetDate ORDER BY sf.branch.id")
    List<SalesForecast> findAllByPeriod(@Param("targetDate") Date targetDate);

    /**
     * 특정 지점의 최신 예상 매출 조회
     */
    Optional<SalesForecast> findFirstByBranchIdOrderByCreatedAtDesc(Long branchId);
}

