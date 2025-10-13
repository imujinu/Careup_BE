package com.careup.branch.domain.auth.dto.response;

import lombok.*;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRefreshResponse {
    private String tokenType;         // "Bearer"
    private String accessToken;       // 새 AT
    private int expiresInMinutes;     // AT 만료(분)

    private Long employeeId;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private Long branchId;
    private String branchName;

    private Instant issuedAt;         // 재발급 시각
}
