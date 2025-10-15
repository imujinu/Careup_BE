package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.response.MemberMyPageDto;
import com.careup.ordering.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
public class MemberQueryController {

    private final MemberQueryService memberQueryService;

    @GetMapping("/my-page")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CommonSuccessDto> myPage(@AuthenticationPrincipal Long memberId) {
        MemberMyPageDto result = memberQueryService.getMyPage(memberId);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("고객 마이페이지 조회")
                        .build()
        );
    }
}
