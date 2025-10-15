package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.request.ForgotPasswordRequest;
import com.careup.branch.domain.auth.dto.request.LogoutRequest;
import com.careup.branch.domain.auth.dto.request.RefreshRequest;
import com.careup.branch.domain.auth.dto.request.ResetPasswordRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.branch.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.branch.domain.auth.service.AuthService;
import jakarta.validation.Valid;
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
    private final JwtTokenProvider jwt; // NEW

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
                        .status_message("액세스 토큰 재발급 완료")
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
                        .status_message("로그아웃 완료")
                        .build()
        );
    }

    /** 비밀번호 재설정 메일 요청 */
    @PostMapping("/password/forgot")
    public ResponseEntity<CommonSuccessDto> forgot(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.issueResetTokenByIdentity(req.getEmail().trim(), req.getMobile().trim());
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(200)
                        .status_message("비밀번호 재설정 메일 전송 완료 (15분 내 유효)")
                        .build()
        );
    }

    /** 비밀번호 재설정 완료 */
    @PostMapping("/password/reset")
    public ResponseEntity<CommonSuccessDto> reset(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(
                req.getEmail().trim(),
                req.getToken().trim(),
                req.getNewPassword().trim(),
                req.getConfirmPassword().trim()
        );
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(200)
                        .status_message("비밀번호 재설정 완료")
                        .build()
        );
    }
}
