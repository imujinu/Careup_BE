// src/main/java/com/careup/ordering/domain/auth/oauth/dto/OauthUpdateRequest.java
package com.careup.ordering.domain.auth.oauth.dto;

import com.careup.ordering.domain.member.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthUpdateRequest {

    @NotBlank
    private String oauthTempToken; // 임시 토큰(Redis)

    // 선택 입력: 카카오처럼 이메일이 없을 때 수집
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 2, max = 30)
    private String name;

    @NotBlank
    @Size(min = 2, max = 10)
    private String nickname;

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
