package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.request.CustomerIdentityChangeRequest;
import com.careup.ordering.domain.member.dto.response.CustomerIdentityChangeResponse;
import com.careup.ordering.domain.member.service.CustomerIdentityCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class CustomerIdentityChangeController {

    private final CustomerIdentityCommandService service;

    @PutMapping("/customers/me/identity")
    public ResponseEntity<CommonSuccessDto> changeMyIdentity(
            @RequestBody @Valid CustomerIdentityChangeRequest req
    ) {
        CustomerIdentityChangeResponse res = service.changeMyIdentity(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("고객 아이디 변경 완료")
                        .result(res)
                        .build()
        );
    }
}
