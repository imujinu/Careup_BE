package com.careup.ordering.domain.auth.oauth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvideCodeRequest {
    @NotEmpty(message = "인가 코드를 입력해 주세요.")
    private String code;

    // 프론트에서 생성한 PKCE code_verifier(구글 권장). Kakao는 비워도 됨.
    private String codeVerifier;

    // 프론트가 생성·검증하는 CSRF 방지용 state. 서버는 수신만(선택).
    private String state;
}
