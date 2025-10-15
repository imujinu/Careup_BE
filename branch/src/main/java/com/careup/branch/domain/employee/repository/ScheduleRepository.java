package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate"})
    List<Schedule> findByEmployeeAndRegisteredDateBetweenOrderByRegisteredDateAsc(
            Employee employee, LocalDate from, LocalDate to
    );

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate"})
    List<Schedule> findByEmployeeAndRegisteredDateBetween(
            Employee employee, LocalDate from, LocalDate to
    );

    boolean existsByScheduleTypeId(Long scheduleTypeId);

    @EntityGraph(attributePaths = {"scheduleType", "branch"})
    List<Schedule> findByEmployeeAndRegisteredDateIn(Employee employee, Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    List<Schedule> findByEmployeeInAndRegisteredDateIn(Collection<Employee> employees, Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    List<Schedule> findByRegisteredDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    List<Schedule> findByRegisteredDateIn(Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    Page<Schedule> findByRegisteredDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    // 추가: 브랜치 IN + 기간 — 브랜치/가맹 관리자 전사 조회 최적화
    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    List<Schedule> findByBranch_IdInAndRegisteredDateBetween(Collection<Long> branchIds, LocalDate from, LocalDate to);
}
