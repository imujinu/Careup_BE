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
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BranchKpiService {
    private final BranchKpiRepository branchKpiRepository;
    private final KpiRepository kpiRepository;
    private final EntityManager em;
    private final KpiCalculationService kpiCalculationService;
    private final KpiVariableService kpiVariableService;

    // 지점별 KPI 생성
    public BranchKpiCreateResDto createBranchKpi(BranchKpiCreateReqDto request) {
        KPI kpi = kpiRepository.findById(request.getKpiId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));
        Branch branch = em.getReference(Branch.class, request.getBranchId());

        // 공식이 있는 경우 자동 계산
        BigDecimal currentValue = request.getCurrentValue();
        if (kpi.getCalculationFormula() != null && !kpi.getCalculationFormula().isEmpty()) {
            try {
                // 변수 데이터 조회
                Map<String, Double> variables = kpiVariableService.getKpiVariables(
                    request.getBranchId(),
                    request.getStartDate(),
                    request.getEndDate()
                );

                // 공식 계산
                currentValue = kpiCalculationService.calculateFormula(
                    kpi.getCalculationFormula(),
                    variables
                );
                log.info("KPI 공식 계산 완료 - KPI ID: {}, 결과: {}", kpi.getId(), currentValue);
            } catch (Exception e) {
                log.error("KPI 공식 계산 실패: {}", e.getMessage(), e);
                // 계산 실패 시 요청값 사용 또는 0
                currentValue = request.getCurrentValue() != null ? request.getCurrentValue() : BigDecimal.ZERO;
            }
        }

        BranchKpi branchKpi = BranchKpi.builder()
                .kpiId(kpi)
                .branchId(branch)
                .targetValue(request.getTargetValue())
                .currentValue(currentValue)
                .achievementRate(calcAchievementRate(currentValue, request.getTargetValue()))
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

        // 공식이 있는 경우 자동 계산
        BigDecimal currentValue = request.getCurrentValue();
        if (kpi.getCalculationFormula() != null && !kpi.getCalculationFormula().isEmpty()) {
            try {
                // 변수 데이터 조회
                Map<String, Double> variables = kpiVariableService.getKpiVariables(
                    request.getBranchId(),
                    request.getStartDate(),
                    request.getEndDate()
                );

                // 공식 계산
                currentValue = kpiCalculationService.calculateFormula(
                    kpi.getCalculationFormula(),
                    variables
                );
                log.info("KPI 공식 계산 완료 - KPI ID: {}, 결과: {}", kpi.getId(), currentValue);
            } catch (Exception e) {
                log.error("KPI 공식 계산 실패: {}", e.getMessage(), e);
                // 계산 실패 시 요청값 사용 또는 기존값
                currentValue = request.getCurrentValue() != null ? request.getCurrentValue() : branchKpi.getCurrentValue();
            }
        }

        // 엔티티의 업데이트 메서드 사용
        branchKpi.updateBranchKpi(
                kpi,
                branch,
                request.getTargetValue(),
                currentValue,
                calcAchievementRate(currentValue, request.getTargetValue()),
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
