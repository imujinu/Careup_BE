package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.request.FindCustomerIdRequest;
import com.careup.ordering.domain.member.dto.response.FindCustomerIdResponse;
import com.careup.ordering.domain.member.service.CustomerIdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/auth/customers")
public class CustomerIdentityController {

    private final CustomerIdentityService identityService;

    @PostMapping("/id/find")
    public ResponseEntity<CommonSuccessDto> findCustomerId(
            @RequestBody @Valid FindCustomerIdRequest req,
            @RequestParam(name = "mask", defaultValue = "false") boolean mask
    ) {
        CustomerIdentityService.Result r = identityService.findCustomerId(req);

        String email = mask ? maskEmail(r.email()) : r.email();
        String phone = mask ? maskPhone(r.phone()) : r.phone();

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message(mask ? "고객 아이디 조회 완료(마스킹)" : "고객 아이디 조회 완료")
                        .result(FindCustomerIdResponse.builder()
                                .email(email)
                                .phone(phone)
                                .build())
                        .build()
        );
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return email; // @ 없거나 맨 앞이면 패스
        String id = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (id.length() == 1) return id + "***@" + domain;
        if (id.length() == 2) return id.substring(0, 1) + "***@" + domain; // ★ 버그 수정: 별 1개 추가하던 오류 제거
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
