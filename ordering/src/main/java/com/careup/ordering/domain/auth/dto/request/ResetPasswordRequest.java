package com.careup.ordering.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String token;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
