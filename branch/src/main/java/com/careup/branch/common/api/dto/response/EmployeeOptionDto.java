package com.careup.branch.common.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeOptionDto {
    private Long id;
    private String name;
    private String employeeNumber;
    private String jobGradeName;
    private List<String> branchNames;
}
