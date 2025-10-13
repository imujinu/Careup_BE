package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.kpi.BranchKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchKpiRepository extends JpaRepository<BranchKpi, Long> {
    // 필요시 커스텀 메서드 추가
}

