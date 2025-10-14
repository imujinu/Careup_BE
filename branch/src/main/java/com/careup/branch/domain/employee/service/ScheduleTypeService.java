package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.ScheduleTypeCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleTypeUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleTypeDetailDto;
import com.careup.branch.domain.employee.entity.ScheduleType;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.ScheduleTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleTypeService {

    private final ScheduleTypeRepository scheduleTypeRepository;
    private final ScheduleRepository scheduleRepository;

    public Page<ScheduleTypeDetailDto> list(Pageable pageable) {
        return scheduleTypeRepository.findAll(pageable).map(ScheduleTypeDetailDto::fromEntity);
    }

    public ScheduleTypeDetailDto detail(Long id) {
        ScheduleType st = scheduleTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("스케줄 종류를 찾을 수 없습니다."));
        return ScheduleTypeDetailDto.fromEntity(st);
    }

    @Transactional
    public ScheduleTypeDetailDto create(ScheduleTypeCreateDto dto) {
        if (scheduleTypeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }
        ScheduleType saved = scheduleTypeRepository.save(
                ScheduleType.builder()
                        .name(dto.getName())
                        .category(dto.getCategory())
                        .build()
        );
        return ScheduleTypeDetailDto.fromEntity(saved);
    }

    @Transactional
    public ScheduleTypeDetailDto update(Long id, ScheduleTypeUpdateDto dto) {
        ScheduleType target = scheduleTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("스케줄 종류를 찾을 수 없습니다."));
        if (!target.getName().equals(dto.getName()) && scheduleTypeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }
        target.update(dto.getName(), dto.getCategory());
        return ScheduleTypeDetailDto.fromEntity(target);
    }

    @Transactional
    public void delete(Long id) {
        if (scheduleRepository.existsByScheduleTypeId(id)) {
            throw new IllegalStateException("이미 사용 중인 스케줄 종류는 삭제할 수 없습니다.");
        }
        ScheduleType target = scheduleTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("스케줄 종류를 찾을 수 없습니다."));
        scheduleTypeRepository.delete(target);
    }
}
