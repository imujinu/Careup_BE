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
    private String employeeNumber;    // 모달 subtitle(사번)
    private String jobGradeName;      // 직급명(선택)
    private List<String> branchNames; // 배치 지점명 리스트(선택)
}
