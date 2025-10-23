package com.careup.ordering.domain.auth.dto.request;

import com.careup.ordering.domain.member.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

    @NotBlank
    @Size(min = 2, max = 30)
    private String name;

    @NotNull
    private LocalDate birthday;

    @NotBlank
    private String phone; // 하이픈 유무 무관

    @NotNull
    private Gender gender; // M or W

    @NotBlank
    @Size(max = 10)
    private String zipcode;

    @NotBlank
    @Size(max = 200)
    private String address;

    @NotBlank
    @Size(max = 200)
    private String addressDetail;
}
