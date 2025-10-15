package com.careup.ordering.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginRequest {
    /** 이메일 또는 휴대폰번호(하이픈 무관) */
    @NotBlank
    private String id;

    @NotBlank
    private String password;

    /** 자동로그인(RefreshToken 발급 여부) */
    private boolean rememberMe;
}
