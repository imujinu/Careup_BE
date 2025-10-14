package com.careup.ordering.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerIdentityChangeRequest {

    @NotBlank
    private String currentPassword;

    @Email
    private String newEmail;

    private String newPhone; // 숫자만 권장
}
