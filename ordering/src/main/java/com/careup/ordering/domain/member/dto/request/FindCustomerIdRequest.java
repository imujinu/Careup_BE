package com.careup.ordering.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindCustomerIdRequest {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate birthday; // yyyy-MM-dd

    @NotBlank
    private String nickname; // 고객 식별값(유니크)
}
