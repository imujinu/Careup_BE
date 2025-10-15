package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeIdLookupRequest {
    @NotBlank
    private String name;

    @NotNull
    private LocalDate dateOfBirth;

    @NotBlank
    private String employeeNumber;  // 유니크 키
}
