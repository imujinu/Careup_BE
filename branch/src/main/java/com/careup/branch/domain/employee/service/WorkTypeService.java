package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.WorkTypeUpsertDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import com.careup.branch.domain.employee.entity.WorkType;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkTypeService {

    private final WorkTypeRepository workTypeRepository;

    public Page<WorkTypeDetailDto> list(Pageable pageable) {
        return workTypeRepository.findAll(pageable).map(WorkTypeDetailDto::fromEntity);
    }

    public WorkTypeDetailDto detail(Long id) {
        WorkType w = workTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("근무 종류를 찾을 수 없습니다."));
        return WorkTypeDetailDto.fromEntity(w);
    }

    @Transactional
    public WorkTypeDetailDto create(WorkTypeUpsertDto dto) {
        if (workTypeRepository.existsByNameIgnoreCase(dto.getName())) throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        WorkType saved = workTypeRepository.save(
                WorkType.builder()
                        .name(dto.getName())
                        .geofenceRequired(Boolean.TRUE.equals(dto.getGeofenceRequired()))
                        .geofenceRadiusMeters(dto.getGeofenceRadiusMeters())
                        .build()
        );
        return WorkTypeDetailDto.fromEntity(saved);
    }

    @Transactional
    public WorkTypeDetailDto update(Long id, WorkTypeUpsertDto dto) {
        WorkType target = workTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("근무 종류를 찾을 수 없습니다."));
        if (!target.getName().equalsIgnoreCase(dto.getName()) && workTypeRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }
        target = WorkType.builder()
                .id(target.getId())
                .name(dto.getName())
                .geofenceRequired(Boolean.TRUE.equals(dto.getGeofenceRequired()))
                .geofenceRadiusMeters(dto.getGeofenceRadiusMeters())
                .build();
        return WorkTypeDetailDto.fromEntity(workTypeRepository.save(target));
    }

    @Transactional
    public void delete(Long id) {
        workTypeRepository.deleteById(id);
    }
}
