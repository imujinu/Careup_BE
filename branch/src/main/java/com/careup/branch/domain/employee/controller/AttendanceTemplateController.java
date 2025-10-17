package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.AttendanceTemplateUpsertDto;
import com.careup.branch.domain.employee.dto.request.AttendanceTemplateUpdateDto;
import com.careup.branch.domain.employee.dto.response.AttendanceTemplateDetailDto;
import com.careup.branch.domain.employee.dto.response.AttendanceTemplateListDto;
import com.careup.branch.domain.employee.service.AttendanceTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance-template")
@RequiredArgsConstructor
public class AttendanceTemplateController {

    private final AttendanceTemplateService attendanceTemplateService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(Pageable pageable) {
        Page<AttendanceTemplateListDto> result = attendanceTemplateService.list(pageable);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 템플릿 목록 조회 완료")
                        .build()
        );
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long id) {
        AttendanceTemplateDetailDto result = attendanceTemplateService.detail(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 템플릿 상세 조회 완료")
                        .build()
        );
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> create(@RequestBody @Valid AttendanceTemplateUpsertDto req) {
        AttendanceTemplateDetailDto result = attendanceTemplateService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("스케줄 템플릿 생성 완료")
                        .build()
        );
    }

    @PatchMapping("/update/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> update(@PathVariable Long id,
                                                   @RequestBody @Valid AttendanceTemplateUpdateDto req) {
        AttendanceTemplateDetailDto result = attendanceTemplateService.update(id, req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 템플릿 수정 완료")
                        .build()
        );
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        attendanceTemplateService.delete(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 템플릿 삭제 완료")
                        .build()
        );
    }
}
