package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTypeRepository extends JpaRepository<ScheduleType, Long> {
    boolean existsByName(String name);
}
