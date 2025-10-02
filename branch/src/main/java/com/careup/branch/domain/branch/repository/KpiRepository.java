package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.KPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KpiRepository extends JpaRepository<KPI, Long> {
    boolean existsByName(String name);
}
