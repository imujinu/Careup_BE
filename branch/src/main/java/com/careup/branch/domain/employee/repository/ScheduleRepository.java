package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByEmployeeAndRegisteredDateBetweenOrderByRegisteredDateAsc(
            Employee employee, LocalDate from, LocalDate to
    );

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByEmployeeAndRegisteredDateBetween(
            Employee employee, LocalDate from, LocalDate to
    );

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByEmployeeAndRegisteredDateIn(Employee employee, Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByEmployeeInAndRegisteredDateIn(Collection<Employee> employees, Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByRegisteredDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByRegisteredDateIn(Collection<LocalDate> dates);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    Page<Schedule> findByRegisteredDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByBranch_IdInAndRegisteredDateBetween(Collection<Long> branchIds, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    Optional<Schedule> findByEmployeeIdAndRegisteredDate(Long employeeId, LocalDate registeredDate);

    // ===== 전파 로직용 추가 메서드 =====
    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByAttendanceTemplate(AttendanceTemplate template);

    @EntityGraph(attributePaths = {"employee", "attendanceTemplate", "branch", "workType", "leaveType"})
    List<Schedule> findByAttendanceTemplateAndRegisteredDateBetween(
            AttendanceTemplate template, LocalDate from, LocalDate to
    );


    Optional<Schedule> findByEmployeeAndRegisteredDate(Employee employee, LocalDate today);
}
