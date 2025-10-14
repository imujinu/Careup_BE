package com.careup.ordering.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthLoginResponse {
    private String tokenType;      // "Bearer"
    private String accessToken;
    private String refreshToken;   // rememberMe=false면 null 가능
    private int expiresInMinutes;  // AT 만료(분)

    private Long memberId;
    private String role;           // CUSTOMER 고정
    private String name;
    private String email;
    private String nickname;
    private String phone;
}
