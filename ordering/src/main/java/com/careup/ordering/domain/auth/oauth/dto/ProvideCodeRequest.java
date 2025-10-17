package com.careup.ordering.domain.auth.oauth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvideCodeRequest {
    @NotEmpty(message = "인가 코드를 입력해 주세요.")
    private String code;
}
