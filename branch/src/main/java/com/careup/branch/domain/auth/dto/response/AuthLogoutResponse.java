package com.careup.branch.domain.auth.dto.response;

import lombok.*;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthLogoutResponse {
    private boolean tokenRevoked;     // 항상 true
    private Long employeeId;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private Long branchId;
    private String branchName;
    private Instant revokedAt;        // 로그아웃 시각
}
