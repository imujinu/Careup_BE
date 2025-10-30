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
import java.util.Collection;
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

    @EntityGraph(attributePaths = {"jobGrade", "dispatchStatuses", "dispatchStatuses.branch"})
    Optional<Employee> findWithDispatchStatusesById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Employee e set e.jobGrade = null where e.jobGrade.id = :jobGradeId")
    int detachJobGradeById(@Param("jobGradeId") Long jobGradeId);

    boolean existsByJobGradeId(Long jobGradeId);

    Optional<Employee> findByNameAndDateOfBirthAndEmployeeNumber(String name, LocalDate dateOfBirth, String employeeNumber);

    @EntityGraph(attributePaths = {"jobGrade"})
    @Query("""
           select e from Employee e
           where lower(e.name) like lower(concat('%', :keyword, '%'))
              or lower(e.employeeNumber) like lower(concat('%', :keyword, '%'))
              or lower(e.email) like lower(concat('%', :keyword, '%'))
           """)
    Page<Employee> searchByKeyword(String keyword, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    boolean existsByEmployeeNumber(String employeeNumber);

    @EntityGraph(attributePaths = {"jobGrade"})
    List<Employee> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"jobGrade"})
    Page<Employee> findByEnabledTrue(Pageable pageable);

    @Query("""
           select (count(e) > 0) from Employee e
           where lower(e.name) like lower(concat('%', :keyword, '%'))
              or lower(e.employeeNumber) like lower(concat('%', :keyword, '%'))
              or lower(e.email) like lower(concat('%', :keyword, '%'))
           """)
    boolean existsByKeyword(@Param("keyword") String keyword);

    boolean existsByJobGrade_Id(Long jobGradeId);
}
