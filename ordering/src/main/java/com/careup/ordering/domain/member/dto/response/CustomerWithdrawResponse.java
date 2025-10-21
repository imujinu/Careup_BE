package com.careup.ordering.domain.member.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerWithdrawResponse {
    private boolean deactivated;
    private Long memberId;
    private String email;
    private Instant deactivatedAt;
}
