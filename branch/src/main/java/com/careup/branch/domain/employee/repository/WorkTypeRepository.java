package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.WorkType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkTypeRepository extends JpaRepository<WorkType, Long> {
    boolean existsByNameIgnoreCase(String name);
}
