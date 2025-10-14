package com.careup.ordering.domain.auth.dto.request;

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
    private String email;
    @NotBlank
    private String mobile; // 하이픈 유무 무관
}
