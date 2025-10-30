package com.careup.branch.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken; // rememberMe=false면 null
    private int expiresInMinutes;
    private String role;
    private Long employeeId;

    private String name;
    private String title; // 프론트에서 미사용 가능(유지)
    private String email;
    private String mobile;

    private Long branchId;
    private String branchName;

    // ★ 헤더 사진 표기를 위한 프로필 이미지 URL
    private String profileImageUrl;
}
