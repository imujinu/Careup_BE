package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.kpi.BranchKpi;
import com.careup.branch.domain.branch.entity.kpi.KpiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchKpiRepository extends JpaRepository<BranchKpi, Long> {

    // KPI별 전체 지점 수 조회
    @Query("SELECT COUNT(DISTINCT bk.branchId.id) FROM BranchKpi bk WHERE bk.kpiId.id = :kpiId")
    Long countTotalBranchesByKpiId(@Param("kpiId") Long kpiId);

    // KPI별 특정 상태의 지점 수 조회
    @Query("SELECT COUNT(DISTINCT bk.branchId.id) FROM BranchKpi bk WHERE bk.kpiId.id = :kpiId AND bk.kpiStatus = :status")
    Long countBranchesByKpiIdAndStatus(@Param("kpiId") Long kpiId, @Param("status") KpiStatus status);

    // KPI별 평균 달성률 조회
    @Query("SELECT AVG(bk.achievementRate) FROM BranchKpi bk WHERE bk.kpiId.id = :kpiId")
    Double getAverageAchievementRateByKpiId(@Param("kpiId") Long kpiId);

    // KPI별 평균 현재값 조회
    @Query("SELECT AVG(bk.currentValue) FROM BranchKpi bk WHERE bk.kpiId.id = :kpiId")
    Double getAverageCurrentValueByKpiId(@Param("kpiId") Long kpiId);

    // KPI별 평균 목표값 조회
    @Query("SELECT AVG(bk.targetValue) FROM BranchKpi bk WHERE bk.kpiId.id = :kpiId")
    Double getAverageTargetValueByKpiId(@Param("kpiId") Long kpiId);
}

