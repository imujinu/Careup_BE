package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.response.MemberDetailDto;
import com.careup.ordering.domain.member.dto.response.MemberListDto;
import com.careup.ordering.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/customers")
public class AdminMemberQueryController {

    private final MemberQueryService memberQueryService;

    // 목록 조회: 본점/지점/가맹 관리자만
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 20)
            Pageable pageable
    ) {
        Page<MemberListDto> result = memberQueryService.getMemberList(pageable);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("고객 목록 조회")
                        .build()
        );
    }

    // 상세 조회: 관리자 전용
    @GetMapping("/detail/{memberId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long memberId) {
        MemberDetailDto detail = memberQueryService.getMemberDetail(memberId);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(detail)
                        .status_code(HttpStatus.OK.value())
                        .status_message("고객 상세 조회")
                        .build()
        );
    }
}
