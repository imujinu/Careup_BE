package com.careup.ordering.domain.auth.oauth.dto;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthLoginResponse {
    private String status;          // "COMPLETE" | "INCOMPLETE"
    private String tokenType;       // "Bearer" (COMPLETE일 때)
    private String accessToken;     // COMPLETE일 때 발급
    private String refreshToken;    // COMPLETE일 때 발급
    private Integer expiresInMinutes;

    private Long memberId;          // COMPLETE일 때 세팅
    private String role;            // "CUSTOMER"

    private String email;
    private String name;
    private String nickname;
    private String phone;

    private String provider;        // "GOOGLE" | "KAKAO"
    private String socialId;

    private String oauthTempToken;  // INCOMPLETE일 때 세팅
}
