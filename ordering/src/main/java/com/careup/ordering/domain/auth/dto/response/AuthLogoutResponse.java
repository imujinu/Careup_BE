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
public class AuthLogoutResponse {
    private boolean tokenRevoked;
    private Long memberId;
    private String email;
    private String nickname;
    private Instant revokedAt;
}
