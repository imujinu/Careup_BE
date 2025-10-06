package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.Employee;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDetailDto {
    private Long id;
    private String employeeNumber;
    private String name;
    private String jobGradeName;
    private LocalDate dateOfBirth;
    private com.careup.branch.domain.employee.entity.Gender gender;
    private String email;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String mobile;
    private String emergencyTel;
    private String emergencyName;
    private com.careup.branch.domain.employee.entity.Relationship relationship;
    private LocalDate hireDate;
    private LocalDate terminateDate;
    private com.careup.branch.domain.employee.entity.AuthorityType authorityType;
    private com.careup.branch.domain.employee.entity.EmploymentStatus employmentStatus;
    private com.careup.branch.domain.employee.entity.EmploymentType employmentType;
    private String profileImageUrl;
    private String remark;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<EmployeeDispatchDto> dispatches;

    public static EmployeeDetailDto fromEntity(Employee e, List<EmployeeDispatchDto> dispatchDtos) {
        return EmployeeDetailDto.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .name(e.getName())
                .jobGradeName(e.getJobGrade() != null ? e.getJobGrade().getName() : null)
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .email(e.getEmail())
                .zipcode(e.getZipcode())
                .address(e.getAddress())
                .addressDetail(e.getAddressDetail())
                .mobile(e.getMobile())
                .emergencyTel(e.getEmergencyTel())
                .emergencyName(e.getEmergencyName())
                .relationship(e.getRelationship())
                .hireDate(e.getHireDate())
                .terminateDate(e.getTerminateDate())
                .authorityType(e.getAuthorityType())
                .employmentStatus(e.getEmploymentStatus())
                .employmentType(e.getEmploymentType())
                .profileImageUrl(e.getProfileImageUrl())
                .remark(e.getRemark())
                .enabled(e.getEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .dispatches(dispatchDtos)
                .build();
    }
}
