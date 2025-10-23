package com.careup.ordering.domain.auth.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.auth.dto.request.AuthLoginRequest;
import com.careup.ordering.domain.auth.dto.request.ForgotPasswordRequest;
import com.careup.ordering.domain.auth.dto.request.LogoutRequest;
import com.careup.ordering.domain.auth.dto.request.RefreshRequest;
import com.careup.ordering.domain.auth.dto.request.ResetPasswordRequest;
import com.careup.ordering.domain.auth.dto.request.SignUpRequest;
import com.careup.ordering.domain.auth.dto.response.AuthLoginResponse;
import com.careup.ordering.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.ordering.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.ordering.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/customers")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 완료 후 자동 로그인 토큰까지 발급 */
    @PostMapping("/signup")
    public ResponseEntity<CommonSuccessDto> signUp(@RequestBody @Valid SignUpRequest req) {
        AuthLoginResponse result = authService.signUp(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("회원가입 성공(자동 로그인)")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<CommonSuccessDto> login(@RequestBody @Valid AuthLoginRequest req) {
        AuthLoginResponse result = authService.login(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인 성공")
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonSuccessDto> refresh(@RequestBody @Valid RefreshRequest req) {
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
    public ResponseEntity<CommonSuccessDto> logout(@RequestBody @Valid LogoutRequest req) {
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

    @PostMapping("/password/forgot")
    public ResponseEntity<CommonSuccessDto> forgot(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.issueResetTokenByIdentity(req.getEmail().trim(), req.getMobile().trim());
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
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
                        .status_code(HttpStatus.OK.value())
                        .status_message("비밀번호 재설정 완료")
                        .build()
        );
    }
}
