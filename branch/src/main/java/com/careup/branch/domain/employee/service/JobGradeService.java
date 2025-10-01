package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.JobGradeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeUpdateDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.dto.response.JobGradeOptionDto;
import com.careup.branch.domain.employee.entity.JobGrade;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobGradeService {

    private final JobGradeRepository jobGradeRepository;

    @Transactional
    public JobGradeListDto create(JobGradeCreateDto dto) {
        if (jobGradeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 직급명입니다.");
        }
        JobGrade saved = jobGradeRepository.save(
                JobGrade.builder().name(dto.getName()).build()
        );
        return JobGradeListDto.fromEntity(saved);
    }

    public JobGradeListDto get(Long id) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        return JobGradeListDto.fromEntity(found);
    }

    public Page<JobGradeListDto> list(Pageable pageable) {
        return jobGradeRepository.findAll(pageable).map(JobGradeListDto::fromEntity);
    }

    @Transactional
    public JobGradeListDto update(Long id, JobGradeUpdateDto dto) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        if (!found.getName().equals(dto.getName()) && jobGradeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 직급명입니다.");
        }
        found.update(dto.getName());
        return JobGradeListDto.fromEntity(found);
    }

    @Transactional
    public void delete(Long id) {
        JobGrade found = jobGradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("직급을 찾을 수 없습니다."));
        try {
            jobGradeRepository.delete(found);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("해당 직급을 사용하는 직원이 있어 삭제할 수 없습니다.");
        }
    }

    public List<JobGradeOptionDto> options() {
        return jobGradeRepository.findAll(Sort.by("id"))
                .stream()
                .map(JobGradeOptionDto::fromEntity)
                .toList();
    }
}
