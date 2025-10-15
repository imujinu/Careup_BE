package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.EmployeeIdLookupRequest;
import com.careup.branch.domain.employee.dto.response.EmployeeIdLookupResponse;
import com.careup.branch.domain.employee.service.EmployeeIdLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** POST /public/auth/employees/id/find?mask=true|false */
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/auth/employees")
public class EmployeeIdentityController {

    private final EmployeeIdLookupService employeeIdLookupService;

    @PostMapping("/id/find")
    public ResponseEntity<CommonSuccessDto> find(
            @RequestBody @Valid EmployeeIdLookupRequest req,
            @RequestParam(name = "mask", defaultValue = "false") boolean mask
    ) {
        EmployeeIdLookupResponse res = employeeIdLookupService.lookup(req);

        EmployeeIdLookupResponse body = mask
                ? EmployeeIdLookupResponse.builder()
                .email(maskEmail(res.getEmail()))
                .mobile(maskPhone(res.getMobile()))
                .build()
                : res;

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message(mask ? "직원 아이디 조회 완료(마스킹)" : "직원 아이디 조회 완료")
                        .result(body)
                        .build()
        );
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return email;
        String id = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (id.length() == 1) return id + "***@" + domain;
        if (id.length() == 2) return id.substring(0, 1) + "***@" + domain;
        return id.substring(0, 2) + "***@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) return phone;
        String head = digits.substring(0, 3);
        String tail = digits.substring(digits.length() - 4);
        return head + "-****-" + tail;
    }
}
