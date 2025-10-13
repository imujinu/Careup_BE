package com.careup.ordering.domain.auth.dto.response;

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
    private Long memberId;
    private String role;              // CUSTOMER
    private String email;
    private String nickname;
    private Instant issuedAt;
}
