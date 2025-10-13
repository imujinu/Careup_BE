package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.kpi.*;
import com.careup.branch.domain.branch.service.BranchKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branch-kpi")
@RequiredArgsConstructor
public class BranchKpiController {
    private final BranchKpiService branchKpiService;

    // 지점별 KPI 생성
    @PostMapping
    public ResponseEntity<?> create(@RequestBody BranchKpiCreateReqDto request) {
        try {
            BranchKpiCreateResDto res = branchKpiService.createBranchKpi(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    CommonSuccessDto.builder()
                            .result(res)
                            .status_code(HttpStatus.CREATED.value())
                            .status_message("지점별 KPI가 성공적으로 생성되었습니다.")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점별 KPI 생성 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점별 KPI 목록 조회 (페이지네이션)
    @GetMapping
    public ResponseEntity<?> getList(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            BranchKpiListResDto list = branchKpiService.getBranchKpiList(pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(list)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점별 KPI 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점별 KPI 목록 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점별 KPI 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        try {
            BranchKpiDto dto = branchKpiService.getBranchKpi(id);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(dto)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점별 KPI 조회 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점별 KPI 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점별 KPI 수정
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BranchKpiCreateReqDto request) {
        try {
            BranchKpiCreateResDto res = branchKpiService.updateBranchKpi(id, request);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(res)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점별 KPI 수정 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_MODIFIED.value())
                            .status_message("지점별 KPI 수정 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점별 KPI 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        branchKpiService.deleteBranchKpi(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("지점별 KPI 삭제 완료")
                        .build()
        );
    }
}

