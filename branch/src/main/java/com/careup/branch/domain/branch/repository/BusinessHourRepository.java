package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BusinessHour;
import com.careup.branch.domain.branch.entity.BusinessHourType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHourRepository extends JpaRepository<BusinessHour, Long> {

    BusinessHour findByBranchAndBusinessHourType(Branch branch, BusinessHourType type);
}
