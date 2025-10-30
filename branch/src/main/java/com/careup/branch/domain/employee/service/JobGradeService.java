// src/main/java/com/careup/branch/domain/employee/service/JobGradeService.java
package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.JobGradeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeUpdateDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.dto.response.JobGradeOptionDto;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.JobGrade;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobGradeService {

    private final JobGradeRepository jobGradeRepository;
    private final EmployeeRepository employeeRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "orderIndex", "id", "name", "authorityType", "createdAt", "updatedAt"
    );

    private String readRole() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) throw new EntityNotFoundException("권한을 확인할 수 없습니다.");
        String role = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst().orElse(null);
        if (role == null) throw new EntityNotFoundException("권한을 확인할 수 없습니다.");
        return role;
    }

    private void validateAuthorityForJobGradeMutation(AuthorityType proposed) {
        String role = readRole();
        if ("HQ_ADMIN".equals(role)) return;
        if (proposed != AuthorityType.STAFF) {
            throw new IllegalArgumentException("해당 권한의 직급을 생성/수정할 수 없습니다.");
        }
    }

    /** 정렬 보정: 허용 필드 외 요청 시 기본(orderIndex ASC, id ASC)로 강제 */
    private Sort ensureSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Order.asc("orderIndex"), Sort.Order.asc("id"));
        }
        boolean allAllowed = true;
        for (Sort.Order o : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(o.getProperty())) {
                allAllowed = false;
                break;
            }
        }
        if (!allAllowed) {
            return Sort.by(Sort.Order.asc("orderIndex"), Sort.Order.asc("id"));
        }
        // 클라이언트 정렬 우선 + id 안정화
        return sort.and(Sort.by("id").ascending());
    }

    @Transactional
    public JobGradeListDto create(JobGradeCreateDto dto) {
        if (jobGradeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 직급명입니다.");
        }
        validateAuthorityForJobGradeMutation(dto.getAuthorityType());

        int nextOrder = jobGradeRepository.findTopByOrderByOrderIndexDesc()
                .map(g -> g.getOrderIndex() + 1)
                .orElse(1);

        JobGrade saved = jobGradeRepository.save(
                JobGrade.builder()
                        .name(dto.getName())
                        .authorityType(dto.getAuthorityType())
                        .orderIndex(nextOrder)
                        .build()
        );
        return JobGradeListDto.fromEntity(saved);
    }

    public JobGradeListDto get(Long id) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        return JobGradeListDto.fromEntity(found);
    }

    public Page<JobGradeListDto> list(Pageable pageable) {
        Pageable p = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ensureSort(pageable.getSort())
        );
        return jobGradeRepository.findAll(p).map(JobGradeListDto::fromEntity);
    }

    @Transactional
    public JobGradeListDto update(Long id, JobGradeUpdateDto dto) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        if (!found.getName().equals(dto.getName()) && jobGradeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 직급명입니다.");
        }
        validateAuthorityForJobGradeMutation(dto.getAuthorityType());
        found.update(dto.getName(), dto.getAuthorityType());
        return JobGradeListDto.fromEntity(found);
    }

    @Transactional
    public void delete(Long id) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));

        // 사용 중이면 삭제 금지
        if (employeeRepository.existsByJobGrade_Id(id)) {
            throw new IllegalStateException("해당 직급을 사용하는 직원이 있어 삭제할 수 없습니다.");
        }

        try {
            jobGradeRepository.delete(found);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("해당 직급 삭제 중 제약조건 오류가 발생했습니다.");
        }
    }

    /** 드롭다운 옵션: 항상 orderIndex ASC, id ASC */
    public List<JobGradeOptionDto> options() {
        return jobGradeRepository.findAll(Sort.by(Sort.Order.asc("orderIndex"), Sort.Order.asc("id")))
                .stream()
                .map(JobGradeOptionDto::fromEntity)
                .toList();
    }

    // ===== 순서 변경 =====
    @Transactional
    public void move(Long id, String direction) {
        JobGrade target = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        Integer cur = target.getOrderIndex();
        if (cur == null) cur = 0;

        JobGrade neighbor;
        if ("UP".equalsIgnoreCase(direction)) {
            neighbor = jobGradeRepository.findTopByOrderIndexLessThanOrderByOrderIndexDesc(cur).orElse(null);
        } else if ("DOWN".equalsIgnoreCase(direction)) {
            neighbor = jobGradeRepository.findTopByOrderIndexGreaterThanOrderByOrderIndexAsc(cur).orElse(null);
        } else {
            throw new IllegalArgumentException("direction은 UP 또는 DOWN 이어야 합니다.");
        }

        if (neighbor == null) return; // 끝단이면 무시

        int tmp = target.getOrderIndex();
        target.changeOrderIndex(neighbor.getOrderIndex());
        neighbor.changeOrderIndex(tmp);
        // 트랜잭션 커밋 시 반영
    }

    @Transactional
    public void reorder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<JobGrade> grades = jobGradeRepository.findAllById(ids);
        Map<Long, JobGrade> map = new HashMap<>();
        for (JobGrade g : grades) map.put(g.getId(), g);

        int idx = 1;
        for (Long gid : ids) {
            JobGrade g = map.get(gid);
            if (g != null) g.changeOrderIndex(idx++);
        }
    }
}
