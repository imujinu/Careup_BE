package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeIdentityChangeRequest {

    @NotBlank
    private String currentPassword;

    @Email
    private String newEmail;   // 선택

    private String newMobile;  // 선택 (숫자만 권장)
}
