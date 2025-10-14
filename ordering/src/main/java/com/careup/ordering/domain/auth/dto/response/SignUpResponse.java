package com.careup.ordering.domain.auth.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpResponse {
    private Long memberId;
    private String email;
    private String nickname;
}
