package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.JobGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobGradeRepository extends JpaRepository<JobGrade, Long> {
    boolean existsByName(String name);
    Optional<JobGrade> findByName(String name);
    Optional<JobGrade> findById(Long id);
}
