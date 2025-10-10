package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.request.RefreshRequest;
import com.careup.branch.domain.auth.dto.request.LogoutRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.branch.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.branch.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<CommonSuccessDto> login(@RequestBody AuthLoginRequest req) {
        AuthLoginResponse result = authService.login(req);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인 성공")
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonSuccessDto> refresh(@RequestBody RefreshRequest req) {
        if (!StringUtils.hasText(req.getRefreshToken())) {
            return ResponseEntity.badRequest().body(
                    CommonSuccessDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("refreshToken은 필수입니다.")
                            .build()
            );
        }
        AuthRefreshResponse result = authService.refresh(req.getRefreshToken());
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("액세스 토큰이 재발급되었습니다.")
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonSuccessDto> logout(@RequestBody LogoutRequest req) {
        if (!StringUtils.hasText(req.getRefreshToken())) {
            return ResponseEntity.badRequest().body(
                    CommonSuccessDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("refreshToken은 필수입니다.")
                            .build()
            );
        }
        AuthLogoutResponse result = authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그아웃 되었습니다.")
                        .build()
        );
    }
}
