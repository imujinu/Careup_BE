package com.careup.ordering.domain.auth.dto.response;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private int expiresInMinutes;
    private Long memberId;
    private String role;
    private String name;
    private String email;
    private String nickname;
    private String phone;
}
