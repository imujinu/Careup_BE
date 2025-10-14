package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.EmployeeIdentityChangeRequest;
import com.careup.branch.domain.employee.dto.response.EmployeeIdentityChangeResponse;
import com.careup.branch.domain.employee.service.EmployeeIdentityCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class EmployeeIdentityChangeController {

    private final EmployeeIdentityCommandService service;

    /** 직원 본인 아이디(이메일/휴대폰) 변경 */
    @PutMapping("/employees/me/identity")
    public ResponseEntity<CommonSuccessDto> changeMyIdentity(
            @RequestBody @Valid EmployeeIdentityChangeRequest req
    ) {
        EmployeeIdentityChangeResponse res = service.changeMyIdentity(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 아이디 변경 완료")
                        .result(res)
                        .build()
        );
    }
}
