package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
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

    List<Schedule> findByEmployeeAndRegisteredDateIn(Employee employee, Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "scheduleType", "attendanceTemplate", "branch"})
    List<Schedule> findByEmployeeInAndRegisteredDateIn(Collection<Employee> employees, Collection<LocalDate> dates);
}
