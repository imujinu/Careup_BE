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

    // ===== 기존 메서드 (유지) =====
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

    @EntityGraph(attributePaths = {"jobGrade"})
    @Query("""
           select e from Employee e
           where lower(e.name) like lower(concat('%', :keyword, '%'))
              or lower(e.employeeNumber) like lower(concat('%', :keyword, '%'))
              or lower(e.email) like lower(concat('%', :keyword, '%'))
           """)
    Page<Employee> searchByKeyword(String keyword, Pageable pageable);

    // ===== 추가 보강 메서드들 =====

    // 중복 검사(대소문자 무시) 고속 체크용
    boolean existsByEmailIgnoreCase(String email);

    // 중복 검사(모바일/사번)
    boolean existsByMobile(String mobile);
    boolean existsByEmployeeNumber(String employeeNumber);

    // 대량 조회 시 N+1 방지용 그래프 로딩(IN 쿼리)
    @EntityGraph(attributePaths = {"jobGrade"})
    List<Employee> findByIdIn(Collection<Long> ids);

    // enabled 필터가 필요한 경우를 대비한 편의 메서드 (페이지네이션)
    @EntityGraph(attributePaths = {"jobGrade"})
    Page<Employee> findByEnabledTrue(Pageable pageable);

    // 이름/사번/이메일 복합 키워드 검색의 Count 최적화가 필요할 때를 대비한 exists
    @Query("""
           select (count(e) > 0) from Employee e
           where lower(e.name) like lower(concat('%', :keyword, '%'))
              or lower(e.employeeNumber) like lower(concat('%', :keyword, '%'))
              or lower(e.email) like lower(concat('%', :keyword, '%'))
           """)
    boolean existsByKeyword(@Param("keyword") String keyword);
}
