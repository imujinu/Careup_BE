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
public class AuthLogoutResponse {
    private boolean tokenRevoked; // 항상 true
    private Long employeeId;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private Long branchId;
    private String branchName;
    private Instant revokedAt;
}
