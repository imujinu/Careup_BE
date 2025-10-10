package com.careup.branch.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {
    @NotBlank
    private String id;       // 이메일
    @NotBlank
    private String token;
    @NotBlank
    private String newPassword;
}
