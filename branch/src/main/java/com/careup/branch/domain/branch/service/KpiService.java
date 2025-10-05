package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.kpi.KpiCreateReqDto;
import com.careup.branch.domain.branch.dto.kpi.KpiCreateResDto;
import com.careup.branch.domain.branch.dto.kpi.KpiDto;
import com.careup.branch.domain.branch.dto.kpi.KpiListResDto;
import com.careup.branch.domain.branch.entity.KPI;
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

    // KPI 항목 생성
    public KpiCreateResDto createKpi(KpiCreateReqDto request) {
        // KPI 이름 중복 체크
        if (kpiRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 KPI 이름입니다.");
        }

        // DTO -> Entity
        KPI kpi = KPI.builder()
                .name(request.getName())
                .goal(request.getGoal())
                .progress(request.getProgress())
                .build();

        // DB 저장
        KPI savedKpi = kpiRepository.save(kpi);
        log.info("KPI 생성 결과: {}", savedKpi);

        // Entity -> DTO
        return KpiCreateResDto.builder()
                .id(savedKpi.getId())
                .name(savedKpi.getName())
                .build();
    }

    // 지점별 KPI 상세 조회
    public KpiCreateReqDto getKpiById(Long id) {
        KPI findKpi = kpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI 항목입니다."));

        log.info("KPI 조회 결과: {}", findKpi);

        return KpiCreateReqDto.fromEntity(findKpi);
    }

    // KPI 항목 수정
    public KpiCreateReqDto updateKpi(Long id, KpiCreateReqDto request) {
        KPI kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI 항목입니다."));

        // KPI 이름 중복 체크
        if (!kpi.getName().equals(request.getName()) && kpiRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 KPI 이름입니다.");
        }

        // 항목 수정
        kpi = KPI.builder()
                .name(request.getName())
                .goal(request.getGoal())
                .progress(request.getProgress())
                .build();
        KPI updatedKpi = kpiRepository.save(kpi);
        log.info("KPI 수정 결과: {}", updatedKpi);

        return KpiCreateReqDto.fromEntity(updatedKpi);
    }

    public void deleteKpi(Long kpiId) {
        KPI findKpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 KPI 항목입니다."));

        kpiRepository.delete(findKpi);
    }

    // KPI 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public KpiListResDto getKpiList(Pageable pageable) {
        Page<KPI> findKpis = kpiRepository.findAll(pageable);

        // Entity -> DTO
        Page<KpiDto> kpiListResDtos = findKpis.map(KpiDto::fromEntity);

        log.info("KPI 목록 조회 결과: {}", kpiListResDtos.getContent());

        return KpiListResDto.fromPage(kpiListResDtos);
    }
}
