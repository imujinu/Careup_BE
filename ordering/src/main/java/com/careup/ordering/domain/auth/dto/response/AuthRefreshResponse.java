package com.careup.ordering.domain.auth.dto.response;

import java.time.Instant;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshResponse {
    private String tokenType;
    private String accessToken;
    private int expiresInMinutes;
    private Long memberId;
    private String role;
    private String email;
    private String nickname;
    private Instant issuedAt;
}
