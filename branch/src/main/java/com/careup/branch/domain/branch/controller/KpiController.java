package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.kpi.KpiCreateReqDto;
import com.careup.branch.domain.branch.dto.kpi.KpiCreateResDto;
import com.careup.branch.domain.branch.dto.kpi.KpiListResDto;
import com.careup.branch.domain.branch.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    // kpi 항목 생성 API
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody KpiCreateReqDto request) {
        try {
            KpiCreateResDto kpi = kpiService.createKpi(request);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(kpi)
                            .status_code(HttpStatus.CREATED.value())
                            .status_message("KPI 항목이 성공적으로 추가되었습니다.")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("KPI 항목 추가 실패: {}" + e.getMessage())
                            .build()
            );
        }
    }

    // KPI 항목 조회 API (페이징)
    @GetMapping()
    public ResponseEntity<?> getKpiList(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        try {
            KpiListResDto kpiList = kpiService.getKpiList(pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(kpiList)
                            .status_code(HttpStatus.OK.value())
                            .status_message("KPI 항목 조회 완료")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("KPI 항목 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점별 KPI 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getKpi(@PathVariable Long id) {
        try {
            KpiCreateReqDto kpiDto = kpiService.getKpiById(id);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(kpiDto)
                            .status_code(HttpStatus.OK.value())
                            .status_message("KPI 항목 조회 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("KPI 항목 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // KPI 항목 수정 API
    @PatchMapping("/update/{id}")
    public ResponseEntity<?> updateKpi(@PathVariable Long id, @RequestBody KpiCreateReqDto request) {
        try {
            KpiCreateReqDto updatedKpi = kpiService.updateKpi(id, request);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(updatedKpi)
                            .status_code(HttpStatus.OK.value())
                            .status_message("KPI 항목 수정 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_MODIFIED.value())
                            .status_message("KPI 항목 수정 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // KPI 항목 삭제 API
    @DeleteMapping("/{kpiId}")
    public ResponseEntity<?> delete(@PathVariable Long kpiId) {
        kpiService.deleteKpi(kpiId);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(kpiId)
                        .status_code(HttpStatus.OK.value())
                        .status_message("KPI 항목 삭제 완료")
                        .build()
        );
    }
}
