package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.kpi.*;
import com.careup.branch.domain.branch.entity.kpi.KPI;
import com.careup.branch.domain.branch.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KpiService {
    private final KpiRepository kpiRepository;

    // KPI 템플릿 생성
    public KpiCreateResDto createKpi(KpiCreateReqDto request) {
        // 중복 이름 확인
        if (kpiRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 KPI 이름입니다.");
        }

        KPI kpi = KPI.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .periodType(request.getPeriodType())
                .calculationFormula(request.getCalculationFormula())
                .build();

        KPI saved = kpiRepository.save(kpi);
        return KpiCreateResDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .build();
    }

    // KPI 템플릿 목록 조회
    @Transactional(readOnly = true)
    public KpiListResDto getKpiList(Pageable pageable) {
        Page<KPI> page = kpiRepository.findAll(pageable);
        Page<KpiDto> dtoPage = page.map(KpiDto::fromEntity);
        return KpiListResDto.fromPage(dtoPage);
    }

    // KPI 템플릿 단건 조회
    @Transactional(readOnly = true)
    public KpiCreateReqDto getKpiById(Long id) {
        KPI kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));
        return KpiCreateReqDto.fromEntity(kpi);
    }

    // KPI 템플릿 수정
    public KpiCreateReqDto updateKpi(Long id, KpiCreateReqDto request) {
        KPI kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));

        // 다른 KPI와 이름 중복 확인 (자기 자신 제외)
        if (!kpi.getName().equals(request.getName()) &&
            kpiRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 KPI 이름입니다.");
        }

        KPI updated = KPI.builder()
                .id(kpi.getId())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .periodType(request.getPeriodType())
                .calculationFormula(request.getCalculationFormula())
                .build();

        KPI saved = kpiRepository.save(updated);
        return KpiCreateReqDto.fromEntity(saved);
    }

    // KPI 템플릿 삭제
    public void deleteKpi(Long kpiId) {
        KPI kpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI입니다."));
        // TODO: 해당 KPI를 사용하는 BranchKpi가 있는지 확인 후 삭제 여부 결정
        kpiRepository.delete(kpi);
    }
}
