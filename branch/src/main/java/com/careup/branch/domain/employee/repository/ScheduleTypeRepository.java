package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTypeRepository extends JpaRepository<ScheduleType, Long> {
    boolean existsByName(String name);

    // 대소문자 무시 중복 방지(선택적 추가)
    boolean existsByNameIgnoreCase(String name);
}
