package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.AuthorityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobGradeUpdateDto {
    @NotBlank
    private String name;

    @NotNull
    private AuthorityType authorityType;
}
