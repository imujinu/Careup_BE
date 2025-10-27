package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.EmployeeCreateDto;
import com.careup.branch.domain.employee.dto.request.EmployeeUpdateDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> create(
            @Valid @RequestPart("meta") EmployeeCreateDto meta,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        EmployeeDetailDto result = employeeService.create(meta, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("직원 등록 및 최초 배치 완료")
                        .build()
        );
    }

    @PatchMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> update(
            @PathVariable Long id,
            @Valid @RequestPart("meta") EmployeeUpdateDto meta,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        EmployeeDetailDto result = employeeService.update(id, meta, image);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 수정 완료")
                        .build()
        );
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 비활성화(퇴사) 처리 완료")
                        .build()
        );
    }

    @PatchMapping("/rehire/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> rehire(@PathVariable Long id) {
        EmployeeDetailDto result = employeeService.rehire(id);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("재입사 처리 완료")
                        .build()
        );
    }
}
