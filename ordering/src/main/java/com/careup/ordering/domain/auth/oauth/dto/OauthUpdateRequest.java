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
}
