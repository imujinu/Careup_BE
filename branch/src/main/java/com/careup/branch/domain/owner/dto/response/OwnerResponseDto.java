package com.careup.branch.domain.owner.dto.response;

import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerResponseDto {

    private Long employeeId;
    private String employeeNumber;
    private String name;
    private String email;
    private String mobile;
    private AuthorityType authorityType;
    private Long branchId;
    private String branchName;
    private LocalDate hireDate;

    public static OwnerResponseDto fromEntity(Employee employee, Long branchId, String branchName) {
        return OwnerResponseDto.builder()
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .name(employee.getName())
                .email(employee.getEmail())
                .mobile(employee.getMobile())
                .authorityType(employee.getAuthorityType())
                .branchId(branchId)
                .branchName(branchName)
                .hireDate(employee.getHireDate())
                .build();
    }
}

