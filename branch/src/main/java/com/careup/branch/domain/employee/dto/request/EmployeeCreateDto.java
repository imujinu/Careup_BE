package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCreateDto {

    @NotBlank
    @Size(max = 30)
    private String employeeNumber;

    @NotBlank
    @Size(max = 100)
    private String name;

    private Long jobGradeId;

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Gender gender;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(max = 20)
    private String zipcode;

    @NotBlank
    private String address;

    @NotBlank
    private String addressDetail;

    @NotBlank
    @Size(max = 32)
    private String mobile;

    @NotBlank
    @Size(max = 32)
    private String emergencyTel;

    @NotBlank
    @Size(max = 50)
    private String emergencyName;

    @NotNull
    private Relationship relationship;

    @NotNull
    private LocalDate hireDate;

    private LocalDate terminateDate;

    @NotNull
    private AuthorityType authorityType;

    @NotNull
    private EmploymentStatus employmentStatus;

    @NotNull
    private EmploymentType employmentType;

    private String profileImageUrl;

    @Size(max = 200)
    private String remark;

    @NotBlank
    @Size(min = 8, max = 100)
    private String rawPassword;

    @Valid
    @Size(min = 1, message = "최소 1개 지점 배치가 필요합니다.")
    private List<DispatchAssignmentDto> dispatches;
}
