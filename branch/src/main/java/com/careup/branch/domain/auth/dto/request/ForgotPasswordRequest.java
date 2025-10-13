package com.careup.branch.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {
    @Email
    @NotBlank
    private String email;   // DB의 이메일과 일치해야 함

    @NotBlank
    private String mobile;  // DB의 휴대폰 번호와 일치해야 함(하이픈 유무 무관)
}
