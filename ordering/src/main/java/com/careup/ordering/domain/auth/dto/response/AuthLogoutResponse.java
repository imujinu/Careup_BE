package com.careup.ordering.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthLogoutResponse {
    private boolean tokenRevoked; // 항상 true
    private Long memberId;
    private String email;
    private String nickname;
    private Instant revokedAt;
}
