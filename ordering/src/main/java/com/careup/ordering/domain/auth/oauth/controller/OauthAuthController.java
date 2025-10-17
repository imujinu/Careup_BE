package com.careup.ordering.domain.auth.oauth.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthLoginResponse;
import com.careup.ordering.domain.auth.oauth.dto.OauthUpdateRequest;
import com.careup.ordering.domain.auth.oauth.dto.ProvideCodeRequest;
import com.careup.ordering.domain.auth.oauth.service.OauthAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/customers/oauth")
public class OauthAuthController {

    private final OauthAuthService service;

    @PostMapping("/google")
    public ResponseEntity<CommonSuccessDto> google(@RequestBody @Valid ProvideCodeRequest req) {
        OauthLoginResponse res = service.googleLogin(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("구글 OAuth 처리 완료")
                        .result(res)
                        .build()
        );
    }

    @PostMapping("/kakao")
    public ResponseEntity<CommonSuccessDto> kakao(@RequestBody @Valid ProvideCodeRequest req) {
        OauthLoginResponse res = service.kakaoLogin(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("카카오 OAuth 처리 완료")
                        .result(res)
                        .build()
        );
    }

    @PostMapping("/update")
    public ResponseEntity<CommonSuccessDto> update(@RequestBody @Valid OauthUpdateRequest req) {
        OauthLoginResponse res = service.completeAdditionalInfo(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.OK.value())
                        .status_message("추가정보 입력 완료")
                        .result(res)
                        .build()
        );
    }
}
