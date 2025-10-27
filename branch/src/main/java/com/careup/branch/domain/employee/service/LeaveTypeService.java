package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.LeaveTypeUpsertDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeDetailDto;
import com.careup.branch.domain.employee.entity.LeaveType;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public Page<LeaveTypeDetailDto> list(Pageable pageable) {
        return leaveTypeRepository.findAll(pageable).map(LeaveTypeDetailDto::fromEntity);
    }

    public LeaveTypeDetailDto detail(Long id) {
        LeaveType t = leaveTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("휴가 종류를 찾을 수 없습니다."));
        return LeaveTypeDetailDto.fromEntity(t);
    }

    @Transactional
    public LeaveTypeDetailDto create(LeaveTypeUpsertDto dto) {
        if (leaveTypeRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }
        LeaveType saved = leaveTypeRepository.save(
                LeaveType.builder()
                        .name(dto.getName())
                        .paid(Boolean.TRUE.equals(dto.getPaid()))
                        .build()
        );
        return LeaveTypeDetailDto.fromEntity(saved);
    }

    @Transactional
    public LeaveTypeDetailDto update(Long id, LeaveTypeUpsertDto dto) {
        LeaveType target = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("휴가 종류를 찾을 수 없습니다."));

        if (!target.getName().equalsIgnoreCase(dto.getName())
                && leaveTypeRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다.");
        }

        // 의미 있는 변경 메서드로 갱신 (재조립 저장 금지)
        target.change(dto.getName(), dto.getPaid());
        return LeaveTypeDetailDto.fromEntity(target);
    }

    @Transactional
    public void delete(Long id) {
        leaveTypeRepository.deleteById(id);
    }
}
