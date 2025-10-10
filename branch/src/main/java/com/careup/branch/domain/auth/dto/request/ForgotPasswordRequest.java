package com.careup.branch.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {
    @NotBlank
    private String id;  // 이메일 또는 휴대폰(메일 발송은 이메일 기준)
}
