package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.kpi.*;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.kpi.BranchKpi;
import com.careup.branch.domain.branch.entity.kpi.KPI;
import com.careup.branch.domain.branch.repository.BranchKpiRepository;
import com.careup.branch.domain.branch.repository.KpiRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BranchKpiService {
    private final BranchKpiRepository branchKpiRepository;
    private final KpiRepository kpiRepository;
    private final EntityManager em;

    // 지점별 KPI 생성
    public BranchKpiCreateResDto createBranchKpi(BranchKpiCreateReqDto request) {
        KPI kpi = kpiRepository.findById(request.getKpiId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));
        Branch branch = em.getReference(Branch.class, request.getBranchId());

        BranchKpi branchKpi = BranchKpi.builder()
                .kpiId(kpi)
                .branchId(branch)
                .targetValue(request.getTargetValue())
                .currentValue(request.getCurrentValue())
                .achievementRate(calcAchievementRate(request.getCurrentValue(), request.getTargetValue()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .kpiStatus(request.getKpiStatus())
                .build();

        BranchKpi saved = branchKpiRepository.save(branchKpi);
        return toResDto(saved);
    }

    // 지점별 KPI 단건 조회
    @Transactional(readOnly = true)
    public BranchKpiDto getBranchKpi(Long id) {
        BranchKpi branchKpi = branchKpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점별 KPI입니다."));
        return toDto(branchKpi);
    }

    // 지점별 KPI 수정
    public BranchKpiCreateResDto updateBranchKpi(Long id, BranchKpiCreateReqDto request) {
        BranchKpi branchKpi = branchKpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점별 KPI입니다."));

        KPI kpi = kpiRepository.findById(request.getKpiId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));
        Branch branch = em.getReference(Branch.class, request.getBranchId());

        // 엔티티의 업데이트 메서드 사용
        branchKpi.updateBranchKpi(
                kpi,
                branch,
                request.getTargetValue(),
                request.getCurrentValue(),
                calcAchievementRate(request.getCurrentValue(), request.getTargetValue()),
                request.getStartDate(),
                request.getEndDate(),
                request.getKpiStatus()
        );

        BranchKpi updated = branchKpiRepository.save(branchKpi);
        return toResDto(updated);
    }

    // 지점별 KPI 삭제
    public void deleteBranchKpi(Long id) {
        BranchKpi branchKpi = branchKpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점별 KPI입니다."));
        branchKpiRepository.delete(branchKpi);
    }

    // 지점별 KPI 목록 조회 (페이지네이션)
    @Transactional(readOnly = true)
    public BranchKpiListResDto getBranchKpiList(Pageable pageable) {
        Page<BranchKpi> page = branchKpiRepository.findAll(pageable);
        Page<BranchKpiDto> dtoPage = page.map(this::toDto);
        return BranchKpiListResDto.fromPage(dtoPage);
    }

    // Entity -> DTO 변환
    private BranchKpiDto toDto(BranchKpi entity) {
        return BranchKpiDto.builder()
                .id(entity.getId())
                .kpiId(entity.getKpiId().getId())
                .branchId(entity.getBranchId().getId())
                .targetValue(entity.getTargetValue())
                .currentValue(entity.getCurrentValue())
                .achievementRate(entity.getAchievementRate())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .kpiStatus(entity.getKpiStatus())
                .build();
    }

    private BranchKpiCreateResDto toResDto(BranchKpi entity) {
        return BranchKpiCreateResDto.builder()
                .id(entity.getId())
                .kpiId(entity.getKpiId().getId())
                .branchId(entity.getBranchId().getId())
                .targetValue(entity.getTargetValue())
                .currentValue(entity.getCurrentValue())
                .achievementRate(entity.getAchievementRate())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .kpiStatus(entity.getKpiStatus())
                .build();
    }

    // 달성률 계산
    private Double calcAchievementRate(BigDecimal current, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return current.divide(target, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
    }
}
