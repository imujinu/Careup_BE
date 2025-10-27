package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceTemplateRepository extends JpaRepository<AttendanceTemplate, Long> {
    boolean existsByNameIgnoreCase(String name);
}
