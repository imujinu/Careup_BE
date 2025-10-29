// src/main/java/com/careup/branch/domain/employee/dto/response/JobGradeListDto.java
package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.JobGrade;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobGradeListDto {
    private Long id;
    private String name;
    private AuthorityType authorityType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobGradeListDto fromEntity(JobGrade g) {
        return JobGradeListDto.builder()
                .id(g.getId())
                .name(g.getName())
                .authorityType(g.getAuthorityType())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
