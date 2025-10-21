package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.request.CustomerWithdrawRequest;
import com.careup.ordering.domain.member.dto.response.CustomerWithdrawResponse;
import com.careup.ordering.domain.member.service.CustomerWithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers/me")
public class CustomerWithdrawalController {

    private final CustomerWithdrawalService service;

    /** 고객 회원탈퇴(소프트 삭제) + 카카오 언링크 */
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CommonSuccessDto> withdraw(@RequestBody @Valid CustomerWithdrawRequest req) {
        CustomerWithdrawResponse res = service.withdraw(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원 탈퇴(비활성화) 완료")
                        .result(res)
                        .build()
        );
    }
}
