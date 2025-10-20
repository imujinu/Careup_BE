package com.careup.ordering.domain.auth.dto.response;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {
    private Long memberId;
    private String email;
    private String nickname;
}
