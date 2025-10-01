package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByMobile(String mobile);

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    @EntityGraph(attributePaths = {"jobGrade"})
    Page<Employee> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"jobGrade"})
    Optional<Employee> findById(Long id);
}
