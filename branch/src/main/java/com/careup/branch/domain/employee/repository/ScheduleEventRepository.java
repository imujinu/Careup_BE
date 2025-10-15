package com.careup.branch.domain.employee.repository;

import com.careup.branch.domain.employee.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long> {

    @EntityGraph(attributePaths = {"schedule", "schedule.employee"})
    List<ScheduleEvent> findByScheduleIdIn(Collection<Long> scheduleIds);

    @EntityGraph(attributePaths = {"schedule", "schedule.employee"})
    Optional<ScheduleEvent> findByScheduleId(Long scheduleId);

    long countByScheduleIdIn(Collection<Long> scheduleIds);

    // 경량 존재 확인(선택적 개선)
    boolean existsByScheduleId(Long scheduleId);
}
