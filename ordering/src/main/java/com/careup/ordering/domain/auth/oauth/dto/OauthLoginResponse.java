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

    // 상태: "COMPLETE" | "INCOMPLETE"
    private String status;

    // COMPLETE일 때만 채워짐
    private String tokenType;       // "Bearer"
    private String accessToken;
    private String refreshToken;
    private Integer expiresInMinutes;

    // COMPLETE일 때만 채워짐
    private Long memberId;
    private String role;            // "CUSTOMER"

    // 사용자 정보(가능한 경우 채움)
    private String email;
    private String name;
    private String nickname;
    private String phone;

    // 소셜 식별
    private String provider;        // "GOOGLE" | "KAKAO"
    private String socialId;

    // INCOMPLETE일 때 추가정보 입력을 위한 임시 토큰
    private String oauthTempToken;
}
