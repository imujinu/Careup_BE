package com.careup.branch.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {
    @Email
    @NotBlank
    private String email;            // 토큰과 매칭할 이메일

    @NotBlank
    private String token;            // 메일에 첨부된 토큰

    @NotBlank
    private String newPassword;      // 새 비밀번호

    @NotBlank
    private String confirmPassword;  // 새 비밀번호 확인
}
