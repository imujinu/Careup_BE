package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.WorkTypeUpsertDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import com.careup.branch.domain.employee.service.WorkTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-type")
@RequiredArgsConstructor
public class WorkTypeController {

    private final WorkTypeService workTypeService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(Pageable pageable) {
        Page<WorkTypeDetailDto> result = workTypeService.list(pageable);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("근무 종류 목록 조회 완료")
                        .build()
        );
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long id) {
        WorkTypeDetailDto result = workTypeService.detail(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("근무 종류 상세 조회 완료")
                        .build()
        );
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> create(@RequestBody WorkTypeUpsertDto req) {
        WorkTypeDetailDto result = workTypeService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("근무 종류 생성 완료")
                        .build()
        );
    }

    @PatchMapping("/update/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> update(@PathVariable Long id, @RequestBody WorkTypeUpsertDto req) {
        WorkTypeDetailDto result = workTypeService.update(id, req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("근무 종류 수정 완료")
                        .build()
        );
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        workTypeService.delete(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("근무 종류 삭제 완료")
                        .build()
        );
    }
}
