package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.AttendanceTemplateUpsertDto;
import com.careup.branch.domain.employee.dto.request.AttendanceTemplateUpdateDto;
import com.careup.branch.domain.employee.dto.response.AttendanceTemplateDetailDto;
import com.careup.branch.domain.employee.dto.response.AttendanceTemplateListDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleEvent;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceTemplateService {

    private final AttendanceTemplateRepository attendanceTemplateRepository;

    // 선택 반영을 위한 추가 의존성
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleValidationService validator;

    public Page<AttendanceTemplateListDto> list(Pageable pageable) {
        return attendanceTemplateRepository.findAll(pageable).map(AttendanceTemplateListDto::fromEntity);
    }

    public AttendanceTemplateDetailDto detail(Long id) {
        AttendanceTemplate t = attendanceTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("스케줄 템플릿을 찾을 수 없습니다."));
        return AttendanceTemplateDetailDto.fromEntity(t);
    }

    @Transactional
    public AttendanceTemplateDetailDto create(AttendanceTemplateUpsertDto dto) {
        // 이름 중복 허용: 중복 체크 제거
        AttendanceTemplate saved = attendanceTemplateRepository.save(
                AttendanceTemplate.builder()
                        .name(dto.getName())
                        .defaultClockIn(dto.getDefaultClockIn())
                        .defaultBreakStart(dto.getDefaultBreakStart())
                        .defaultBreakEnd(dto.getDefaultBreakEnd())
                        .defaultClockOut(dto.getDefaultClockOut())
                        .build()
        );
        return AttendanceTemplateDetailDto.fromEntity(saved);
    }

    @Transactional
    public AttendanceTemplateDetailDto update(Long id, AttendanceTemplateUpdateDto dto) {
        AttendanceTemplate target = attendanceTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("스케줄 템플릿을 찾을 수 없습니다."));

        // 이름 중복 허용: 존재 체크 제거

        // 변경 전(old) 템플릿 시간 백업
        LocalTime oldIn  = target.getDefaultClockIn();
        LocalTime oldBs  = target.getDefaultBreakStart();
        LocalTime oldBe  = target.getDefaultBreakEnd();
        LocalTime oldOut = target.getDefaultClockOut();

        // 템플릿 자체 수정
        target.change(
                dto.getName(),
                dto.getDefaultClockIn(),
                dto.getDefaultBreakStart(),
                dto.getDefaultBreakEnd(),
                dto.getDefaultClockOut()
        );

        // 선택: 기존 스케줄 반영
        if (Boolean.TRUE.equals(dto.getPropagate())) {
            propagateToLinkedSchedules(target, oldIn, oldBs, oldBe, oldOut, dto);
        }

        return AttendanceTemplateDetailDto.fromEntity(target);
    }

    @Transactional
    public void delete(Long id) {
        try {
            attendanceTemplateRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("해당 템플릿을 참조하는 스케줄이 있어 삭제할 수 없습니다.");
        }
    }

    /* =========================
       내부: 템플릿 변경사항 스케줄 반영
       ========================= */

    private void propagateToLinkedSchedules(AttendanceTemplate template,
                                            LocalTime oldIn, LocalTime oldBs, LocalTime oldBe, LocalTime oldOut,
                                            AttendanceTemplateUpdateDto dto) {

        // 대상 스케줄 수집
        List<Schedule> candidates = fetchCandidates(template, dto.getPropagateFrom(), dto.getPropagateTo());
        if (candidates.isEmpty()) return;

        // 이벤트 존재 여부로 필터링 맵
        Map<Long, ScheduleEvent> evMap = scheduleEventRepository
                .findByScheduleIdIn(candidates.stream().map(Schedule::getId).toList())
                .stream()
                .collect(Collectors.toMap(e -> e.getSchedule().getId(), e -> e, (a, b) -> a));

        // 변경 후(new) 템플릿 시간
        LocalTime newIn  = template.getDefaultClockIn();
        LocalTime newBs  = template.getDefaultBreakStart();
        LocalTime newBe  = template.getDefaultBreakEnd();
        LocalTime newOut = template.getDefaultClockOut();

        for (Schedule s : candidates) {
            // 1) 근태 이벤트 있으면 스킵
            if (evMap.containsKey(s.getId())) continue;
            // 2) 휴가 스케줄은 스킵
            if (s.getCategory() == ScheduleTypeCategory.LEAVE) continue;

            LocalDate base = s.getRegisteredDate();

            // 템플릿 기반 old/new 시퀀스 계산(날짜 합성 + 자정경계 정규화)
            Seq oldSeq = buildSeq(base, oldIn, oldBs, oldBe, oldOut);
            Seq newSeq = buildSeq(base, newIn, newBs, newBe, newOut);

            // 전략: ONLY_IF_UNMODIFIED ⇒ 현재 등록값이 old 템플릿 파생값과 동일한 경우만 반영
            if (dto.getStrategy() == AttendanceTemplateUpdateDto.PropagationStrategy.ONLY_IF_UNMODIFIED) {
                if (!sameOrNull(s.getRegisteredClockIn(),    oldSeq.in))  continue;
                if (!sameOrNull(s.getRegisteredBreakStart(), oldSeq.bs))  continue;
                if (!sameOrNull(s.getRegisteredBreakEnd(),   oldSeq.be))  continue;
                if (!sameOrNull(s.getRegisteredClockOut(),   oldSeq.out)) continue;
            }

            // 템플릿 값이 null인 항목은 기존 스케줄 값을 유지(coalesce)
            LocalDateTime in  = (newSeq.in  != null) ? newSeq.in  : s.getRegisteredClockIn();
            LocalDateTime bs  = (newSeq.bs  != null) ? newSeq.bs  : s.getRegisteredBreakStart();
            LocalDateTime be  = (newSeq.be  != null) ? newSeq.be  : s.getRegisteredBreakEnd();
            LocalDateTime out = (newSeq.out != null) ? newSeq.out : s.getRegisteredClockOut();

            // out 자정 경계 보정
            if (in != null && out != null && !out.isAfter(in)) {
                out = out.plusDays(1);
            }

            // 충돌 검증(근무 스케줄 기준)
            validator.validateNoConflictOnSave(
                    s.getEmployee(),
                    base,
                    s.getBranch().getId(),
                    false,                 // isLeave = false
                    in,
                    out,
                    s.getId()
            );

            // 실제 반영
            s.change(
                    s.getBranch(),
                    s.getCategory(),
                    s.getWorkType(),
                    s.getLeaveType(),
                    template,
                    base,
                    in,
                    bs,
                    be,
                    out
            );
        }
    }

    private List<Schedule> fetchCandidates(AttendanceTemplate template, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return scheduleRepository.findByAttendanceTemplate(template);
        }
        if (from != null && to != null) {
            return scheduleRepository.findByAttendanceTemplateAndRegisteredDateBetween(template, from, to);
        }
        // 한쪽만 온 경우 보정
        LocalDate f = (from != null) ? from : LocalDate.of(1900, 1, 1);
        LocalDate t = (to   != null) ? to   : LocalDate.of(9999, 12, 31);
        return scheduleRepository.findByAttendanceTemplateAndRegisteredDateBetween(template, f, t);
    }

    /* 템플릿(LocalTime) -> 해당 날짜(LocalDateTime)로 정규화하여 in→bs→be→out 순으로 +1일 보정 */
    private Seq buildSeq(LocalDate baseDate,
                         LocalTime inT, LocalTime bsT, LocalTime beT, LocalTime outT) {
        LocalDateTime in  = at(baseDate, inT);
        LocalDateTime bs  = normalizeAfter(in,  baseDate, bsT);
        LocalDateTime be  = normalizeAfter(bs != null ? bs : in, baseDate, beT);
        LocalDateTime out = normalizeAfter(be != null ? be : (bs != null ? bs : in), baseDate, outT);
        return new Seq(in, bs, be, out);
    }

    private LocalDateTime at(LocalDate d, LocalTime t) {
        return (t == null) ? null : LocalDateTime.of(d, t);
    }

    private LocalDateTime normalizeAfter(LocalDateTime prev, LocalDate base, LocalTime t) {
        if (t == null) return null;
        LocalDateTime cand = LocalDateTime.of(base, t);
        if (prev != null && !cand.isAfter(prev)) cand = cand.plusDays(1);
        return cand;
    }

    private boolean sameOrNull(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static record Seq(LocalDateTime in, LocalDateTime bs, LocalDateTime be, LocalDateTime out) {}
}
