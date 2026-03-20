package com.careup.ordering.domain.statistics.repository;

import com.careup.ordering.domain.statistics.entity.DailyBranchSalesStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyBranchSalesStatisticRepository extends JpaRepository<DailyBranchSalesStatistic, Long> {
    // 기존에 집계된 통계가 있는지 확인하기 위한 메서드
    Optional<DailyBranchSalesStatistic> findByBranchIdAndSalesDate(Long branchId, LocalDate salesDate);
}