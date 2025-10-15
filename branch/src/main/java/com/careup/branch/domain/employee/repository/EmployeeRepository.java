package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Employee e set e.jobGrade = null where e.jobGrade.id = :jobGradeId")
    int detachJobGradeById(@Param("jobGradeId") Long jobGradeId);

    boolean existsByJobGradeId(Long jobGradeId);

    // 리팩토링: 유니크 키 포함(단일 결과)
    Optional<Employee> findByNameAndDateOfBirthAndEmployeeNumber(String name, LocalDate dateOfBirth, String employeeNumber);
}
