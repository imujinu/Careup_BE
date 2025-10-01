package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.JobGradeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeUpdateDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.dto.response.JobGradeOptionDto;
import com.careup.branch.domain.employee.service.JobGradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job-grades")
@RequiredArgsConstructor
public class JobGradeController {

    private final JobGradeService jobGradeService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<JobGradeListDto> result = jobGradeService.list(pageable);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(200)
                        .status_message("직급 목록 조회")
                        .build()
        );
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long id) {
        JobGradeListDto result = jobGradeService.get(id);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직급 상세 조회")
                        .build()
        );
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> create(@Valid @RequestBody JobGradeCreateDto req) {
        JobGradeListDto result = jobGradeService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("직급 등록 완료")
                        .build()
        );
    }

    // ★ '/update/{id}'로 경로 통일
    @PatchMapping("/update/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> update(@PathVariable Long id,
                                                   @Valid @RequestBody JobGradeUpdateDto req) {
        JobGradeListDto result = jobGradeService.update(id, req);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직급 수정 완료")
                        .build()
        );
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        jobGradeService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("직급 삭제 완료")
                        .build()
        );
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> options() {
        List<JobGradeOptionDto> result = jobGradeService.options();
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(200)
                        .status_message("직급 옵션 조회")
                        .build()
        );
    }
}
