package com.careup.branch.domain.auth.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshResponse {
    private String tokenType;
    private String accessToken;
    private int expiresInMinutes;

    private Long employeeId;
    private String name;
    private String email;
    private String mobile;
    private String role;

    private Long branchId;
    private String branchName;

    private Instant issuedAt;

    // ★ 일관성 위해 리프레시 응답에도 포함
    private String profileImageUrl;
}
