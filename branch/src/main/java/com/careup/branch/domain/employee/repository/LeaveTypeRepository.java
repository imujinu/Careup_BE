package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    boolean existsByNameIgnoreCase(String name);
}
