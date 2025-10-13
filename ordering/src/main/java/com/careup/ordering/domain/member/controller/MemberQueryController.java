package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.response.MemberMyPageDto;
import com.careup.ordering.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
public class MemberQueryController {

    private final MemberQueryService customerQueryService;

    @GetMapping("/my-page")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CommonSuccessDto> myPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = (Long) auth.getPrincipal(); // JwtTokenFilter에서 Long으로 설정

        MemberMyPageDto result = customerQueryService.getMyPage(memberId);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("고객 마이페이지 조회")
                        .build()
        );
    }
}
