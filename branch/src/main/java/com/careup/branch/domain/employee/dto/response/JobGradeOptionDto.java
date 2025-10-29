// src/main/java/com/careup/branch/domain/employee/dto/response/JobGradeOptionDto.java
package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.JobGrade;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobGradeOptionDto {
    private Long id;
    private String name;
    private AuthorityType authorityType;

    public static JobGradeOptionDto fromEntity(JobGrade e) {
        return JobGradeOptionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .authorityType(e.getAuthorityType())
                .build();
    }
}
