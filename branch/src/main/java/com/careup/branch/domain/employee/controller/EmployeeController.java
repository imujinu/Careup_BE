package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.EmployeeCreateDto;
import com.careup.branch.domain.employee.dto.request.EmployeeUpdateDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /// 직원 생성 (HQ_ADMIN, BRANCH_ADMIN, FRANCHISE_OWNER) + 최초 배치
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> create(@Valid @RequestBody EmployeeCreateDto req) {
        EmployeeDetailDto result = employeeService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("직원 등록 및 최초 배치 완료")
                        .build()
        );
    }

    /// 직원 수정 (HQ_ADMIN, BRANCH_ADMIN, FRANCHISE_OWNER)
    @PatchMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> update(@PathVariable Long id,
                                                   @Valid @RequestBody EmployeeUpdateDto req) {
        EmployeeDetailDto result = employeeService.update(id, req);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 수정 완료")
                        .build()
        );
    }

    /// 직원 삭제 (HQ_ADMIN, BRANCH_ADMIN, FRANCHISE_OWNER)
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 삭제 완료")
                        .build()
        );
    }
}
