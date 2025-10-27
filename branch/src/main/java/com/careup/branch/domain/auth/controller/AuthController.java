package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.api.ApiEnvelope;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.request.ForgotPasswordRequest;
import com.careup.branch.domain.auth.dto.request.LogoutRequest;
import com.careup.branch.domain.auth.dto.request.RefreshRequest;
import com.careup.branch.domain.auth.dto.request.ResetPasswordRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.branch.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.branch.domain.auth.exception.LoginException;
import com.careup.branch.domain.auth.service.AuthErrorCodes;
import com.careup.branch.domain.auth.service.AuthService;
import com.careup.branch.domain.auth.utils.LoginValidators;
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

    @PostMapping("/login")
    public ResponseEntity<ApiEnvelope<AuthLoginResponse>> login(@RequestBody AuthLoginRequest req) {
        if (!LoginValidators.isValidId(req.getId())) {
            boolean looksEmail = LoginValidators.looksLikeEmail(req.getId());
            String msg = looksEmail ? "잘못된 이메일 형식입니다." : "잘못된 휴대폰 번호 형식입니다.";
            return ResponseEntity.badRequest().body(
                    ApiEnvelope.of(400, msg, AuthErrorCodes.ID_FORMAT_INVALID)
            );
        }
        if (!LoginValidators.isValidPassword(req.getPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiEnvelope.of(400, "비밀번호 형식이 올바르지 않습니다.", AuthErrorCodes.PWD_FORMAT_INVALID)
            );
        }

        try {
            AuthLoginResponse result = authService.login(req);
            return ResponseEntity.ok(ApiEnvelope.ok(result));
        } catch (LoginException e) {
            return ResponseEntity.status(e.getStatus()).body(
                    ApiEnvelope.of(e.getStatus(), e.getMessage(), e.getCode())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiEnvelope.of(401, "아이디 또는 비밀번호가 올바르지 않습니다.", AuthErrorCodes.AUTH_INVALID_CREDENTIALS)
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiEnvelope<AuthRefreshResponse>> refresh(@RequestBody RefreshRequest req) {
        if (!StringUtils.hasText(req.getRefreshToken())) {
            return ResponseEntity.badRequest().body(
                    ApiEnvelope.of(400, "refreshToken은 필수입니다.", AuthErrorCodes.REFRESH_TOKEN_REQUIRED)
            );
        }
        try {
            AuthRefreshResponse result = authService.refresh(req.getRefreshToken());
            return ResponseEntity.ok(ApiEnvelope.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiEnvelope.of(401, "리프레시 토큰이 유효하지 않습니다.", AuthErrorCodes.AUTH_INVALID_CREDENTIALS)
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiEnvelope<AuthLogoutResponse>> logout(@RequestBody LogoutRequest req) {
        if (!StringUtils.hasText(req.getRefreshToken())) {
            return ResponseEntity.badRequest().body(
                    ApiEnvelope.of(400, "refreshToken은 필수입니다.", AuthErrorCodes.REFRESH_TOKEN_REQUIRED)
            );
        }
        try {
            AuthLogoutResponse result = authService.logout(req.getRefreshToken());
            return ResponseEntity.ok(ApiEnvelope.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiEnvelope.of(401, "리프레시 토큰이 유효하지 않습니다.", AuthErrorCodes.AUTH_INVALID_CREDENTIALS)
            );
        }
    }

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
