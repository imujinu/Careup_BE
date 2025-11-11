package com.careup.ordering.domain.member.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.dto.response.MemberMyPageDto;
import com.careup.ordering.domain.member.service.MemberQueryService;
import com.careup.ordering.domain.order.dto.response.ProductViewCountResDto;
import com.careup.ordering.domain.recomendation.dto.ProductViewResultDto;
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

    // 상품 조회 수 업데이트
    @PostMapping("/product/view/{productId}")
    public ResponseEntity<?> viewProduct(@PathVariable Long productId) {
        memberQueryService.viewProduct(productId);
        return new ResponseEntity<>(new CommonSuccessDto("최근 조회 상품 업데이트 성공", HttpStatus.OK.value(), "최근 조회 상품 업데이트 성공"), HttpStatus.OK);
    }

    // 최근 구매 상품 업데이트
    @PostMapping("/product/{productId}")
    public ResponseEntity<?> buyProduct(@PathVariable Long productId, @RequestParam Long memberId) {
        memberQueryService.buyProduct(productId, memberId);
        return new ResponseEntity<>(new CommonSuccessDto("최근 조회 상품 업데이트 성공", HttpStatus.OK.value(), "최근 조회 상품 업데이트 성공"), HttpStatus.OK);
    }


//    // 가장 최근 조회 상품 조회
//    @GetMapping("/product/view/{memberId}")
//    public ResponseEntity<?> getProductId(@PathVariable Long memberId) {
//        ProductViewResultDto productId =  memberQueryService.getProductId(memberId);
//        return new ResponseEntity<>(new CommonSuccessDto(productId, HttpStatus.OK.value(), "최근 조회 상품 ID 조회 완료"), HttpStatus.OK);
//    }
}
